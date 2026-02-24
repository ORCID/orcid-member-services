# src/main/docker/Dockerfile

FROM eclipse-temurin:21-jre-jammy

RUN apt-get update && \
    apt-get install -y iputils-ping python3 curl && \
    rm -rf /var/lib/apt/lists/*

ENV SPRING_OUTPUT_ANSI_ENABLED=ALWAYS

EXPOSE 9010

COPY src/main/docker/entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh && sync

COPY target/*.jar app.jar

ENTRYPOINT ["/entrypoint.sh"]