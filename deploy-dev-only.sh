#!/bin/bash
# Using perl due to sed -i incompatibility in GNU vs FreeBSD
docker image prune -f -a 
perl -i -pe "s/TAG=.*/TAG=$2/" .env
perl -i -pe "s/ENV=.*/ENV=$1/" .env
echo "About to deploy release $2"
docker-compose down
docker-compose up -d
echo "Done"
