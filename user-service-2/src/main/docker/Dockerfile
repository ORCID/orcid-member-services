# src/main/docker/Dockerfile

FROM eclipse-temurin:21-jre-jammy

RUN apt-get update && \
    apt-get install -y iputils-ping python3 curl && \
    rm -rf /var/lib/apt/lists/*

ENV SPRING_OUTPUT_ANSI_ENABLED=ALWAYS

EXPOSE 9000

# add orcid ca to allow Java application to trust other containers
ADD src/main/docker/cacerts /opt/java/openjdk/lib/security/cacerts

# add orcid ca to system to allow curl healthchecks to work
ADD src/main/docker/ca-certificates.crt /etc/ssl/certs/ca-certificates.crt

COPY src/main/docker/entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh && sync

COPY target/*.jar app.jar

ENTRYPOINT ["/entrypoint.sh"]
