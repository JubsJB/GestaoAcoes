ARG RUNTIME_IMAGE
FROM ${RUNTIME_IMAGE}
RUN mkdir -p /run/tls && chmod 700 /run/tls
COPY browser/ /usr/share/nginx/html/
COPY nginx.conf /etc/nginx/conf.d/default.conf
