#!/usr/bin/env bash
set -euo pipefail

COMPOSE="docker compose -f docker-compose-v2.yml"

echo "Building Java service JARs..."
mvn -f user-service-2/pom.xml package -DskipTests -q
mvn -f member-service-2/pom.xml package -DskipTests -q
mvn -f assertion-service-2/pom.xml package -DskipTests -q

echo "Building and starting containers..."
$COMPOSE build
$COMPOSE up -d

