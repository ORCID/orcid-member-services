#!/bin/sh

exec java ${JAVA_OPTS} -XX:+AlwaysPreTouch -Djava.security.egd=file:/dev/./urandom -jar /app.jar "$@"