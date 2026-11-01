ARG RUNTIME_IMAGE
FROM ${RUNTIME_IMAGE}
RUN mkdir -p /run/secrets && chown postgres:postgres /run/secrets && chmod 700 /run/secrets
COPY postgres-init.sh /docker-entrypoint-initdb.d/010-app.sh
