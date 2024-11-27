FROM python:3.11-alpine

ENV DIAL_BASE_URL=''
ENV SOURCES=''
ENV TARGET_DIR=/sources
ENV TEMPLATES_DIR=/templates
ENV API_KEY=''
ENV JWT=''

RUN pip install --no-cache-dir requests==2.32.3 packaging==24.1

WORKDIR /builder

RUN adduser -u 1001 --disabled-password --gecos "" appuser

# Prepare "output" directories for code sources and docker image templates
RUN mkdir ${TARGET_DIR} ${TEMPLATES_DIR} && \
    chown -R appuser:appuser ${TARGET_DIR} ${TEMPLATES_DIR}

COPY . .

RUN chmod +x entrypoint.sh

USER appuser

ENTRYPOINT ["/builder/entrypoint.sh"]
