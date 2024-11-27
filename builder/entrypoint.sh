#!/bin/sh

set -e

cp -r templates/* "$TEMPLATES_DIR"

exec python download_files.py
