ARG JAVA_VERSION=17
ARG BASE_DISTRO=jammy
FROM eclipse-temurin:$JAVA_VERSION-jre-$BASE_DISTRO

ARG UID=1000
ARG GID=1000

VOLUME /data

RUN set -eux; \
	mkdir /data; \
	groupadd -g "${GID}" proxy; \
	useradd --create-home --no-log-init -s /bin/bash -d /proxy -u "${UID}" -g "${GID}" proxy; \
	chown proxy:proxy /data

RUN set -eux; \
	apt-get update; \
    apt-get install -y sudo

COPY --chmod=775 docker-entrypoint.sh /
COPY --chmod=775 send-command /usr/bin/send-command

STOPSIGNAL SIGINT
ENV PROXY_STDIN_PATH=/proxy/stdin
RUN set -eux; \
	mkfifo PROXY_STDIN_PATH; \
	chown proxy:proxy PROXY_STDIN_PATH

USER proxy
WORKDIR /proxy

COPY --chmod=770 ../build/output/midnightproxy.jar /proxy/midnightproxy.jar
COPY --chmod=770 start.sh /proxy/start.sh
COPY --chmod=770 wrapper /proxy/wrapper

USER root
ENTRYPOINT [ "/docker-entrypoint.sh" ]
CMD [ "/proxy/wrapper" ]