ARG RUNTIME_IMAGE
FROM ${RUNTIME_IMAGE}
RUN groupadd -g 10001 app && useradd -u 10001 -g app -s /usr/sbin/nologin app && mkdir -p /run/secrets && chown app:app /run/secrets && chmod 700 /run/secrets
WORKDIR /app
COPY app.jar /app/app.jar
USER 10001:10001
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
