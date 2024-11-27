import os
from pathlib import Path
from typing import Mapping
from urllib.parse import urljoin, unquote

import requests
from packaging.requirements import Requirement


class AppValidationException(Exception):
    pass


def download_files(
        dial_base_url: str,
        headers: Mapping,
        source_base: str | None,
        target: Path,
        files_metadata: list[dict[str, any]],
):
    for file_metadata in files_metadata:
        if file_metadata["resourceType"] == "FILE":
            url = file_metadata["url"]
            file_path = target / unquote(url).removeprefix(source_base)
            download_file(dial_base_url, headers, url, file_path)


def download_file(dial_base_url: str, headers: Mapping, file_url: str, target: Path):
    file_url = urljoin(dial_base_url, "v1/" + file_url)
    with requests.get(file_url, headers=headers, stream=True) as response:
        response.raise_for_status()

        print(f"{file_url} => {target}")
        target.parent.mkdir(parents=True, exist_ok=True)

        with target.open('wb') as file:
            for chunk in response.iter_content(chunk_size=8192):
                file.write(chunk)


def validate_entrypoint(target: Path):
    entrypoint = target / "app.py"
    if not entrypoint.exists():
        raise AppValidationException("Missing entrypoint file: app.py")


def validate_packages(target: Path, allowed_packages: set[str]):
    requirements = target / "requirements.txt"
    requirements.touch(exist_ok=True)

    with open(target / "requirements.txt") as lines:
        for line in lines:
            line = line.strip()
            if not line or line.startswith('#'):
                continue

            requirement = parse_requirement(line)
            package_name = requirement.name
            if requirement.url:
                raise AppValidationException(f"URLs are not allowed in requirements.txt: {requirement.url}")

            if package_name not in allowed_packages:
                raise AppValidationException(f"Package '{package_name}' is forbidden.")


def parse_requirement(line):
    if line.startswith("-"):
        raise AppValidationException(f"Pip options aren't allowed in requirements.txt: {line}")

    try:
        return Requirement(line)
    except Exception as e:
        raise AppValidationException(f"Unsupported requirement: {line}") from e


def main():
    dial_base_url = os.environ["DIAL_BASE_URL"]
    sources = os.environ["SOURCES"]
    target = Path(os.environ["TARGET_DIR"])
    api_key = os.getenv("API_KEY")
    jwt = os.getenv("JWT")
    allowed_packages = os.getenv("ALLOWED_PACKAGES")

    print(f"Dial base url: {dial_base_url}")
    print(f"Sources: {sources}")
    print(f"Target folder: {target}")

    headers: dict[str, str] = {}
    if api_key:
        headers["api-key"] = api_key
    if jwt:
        headers["Authorization"] = f"Bearer {jwt}"

    metadata_url = urljoin(dial_base_url, f"v1/metadata/{sources}")
    params: dict[str, str] = {"recursive": "true"}
    while True:
        with requests.get(metadata_url, params, headers=headers) as response:
            response.raise_for_status()

            result: dict[str, any] = response.json()

            if not result["nodeType"] == "FOLDER":
                raise AppValidationException("Sources path must be a folder")

            download_files(dial_base_url, headers, unquote(sources), target, result.get("items", []))

            token = result.get("nextToken")
            if not token:
                break

            params["token"] = token

    validate_entrypoint(target)
    validate_packages(target, set(allowed_packages.split()))


if __name__ == "__main__":
    main()
