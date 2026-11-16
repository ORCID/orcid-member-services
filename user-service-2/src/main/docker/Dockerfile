# src/main/docker/Dockerfile

FROM eclipse-temurin:21-jre-noble

ENV SPRING_OUTPUT_ANSI_ENABLED=ALWAYS

EXPOSE 9000

# orcid CAs so the JVM and curl trust other containers
COPY src/main/docker/orcid_ec_2022.crt  /usr/local/share/ca-certificates/orcid_ec_2022.crt
COPY src/main/docker/orcid_rsa_2022.crt /usr/local/share/ca-certificates/orcid_rsa_2022.crt

COPY src/main/docker/entrypoint.sh /entrypoint.sh

# update-ca-certificates regenerates both the system bundle (for curl healthchecks)
# and, via ca-certificates-java, the JKS trust store the JVM reads
RUN apt-get update \
 && apt-get install -y --no-install-recommends ca-certificates ca-certificates-java curl iputils-ping python3 \
 && update-ca-certificates \
 && ln -sf /etc/ssl/certs/java/cacerts /opt/java/openjdk/lib/security/cacerts \
 && chmod +x /entrypoint.sh \
 && rm -rf /var/lib/apt/lists/*

COPY target/*.jar app.jar

ENTRYPOINT ["/entrypoint.sh"]
