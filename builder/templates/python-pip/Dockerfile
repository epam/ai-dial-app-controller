ARG PYTHON_IMAGE
FROM $PYTHON_IMAGE

# Allow statements and log messages to immediately appear in the logs
ENV PYTHONUNBUFFERED=True

# Create a non-root user
RUN adduser -u 1001 --disabled-password --gecos "" appuser

WORKDIR /app

COPY requirements.txt .

RUN pip install --no-cache-dir uvicorn==0.31.0 \
    && pip install --no-cache-dir --requirement requirements.txt

COPY --chown=appuser . .

EXPOSE 8080

USER appuser

ENTRYPOINT ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "8080"]
