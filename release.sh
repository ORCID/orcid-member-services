#!/bin/bash
if [ "$2" != "current" ]
then
    echo "Getting latest master"
    git checkout master
    git pull
    if [ "$3" = "tagRelease" ]
    then
        echo "Creating tag $2"
        git tag $2
        git push origin --tags --no-verify
    else
        git checkout $2
    fi
fi
# Using perl due to sed -i incompatibility in GNU vs FreeBSD
perl -i -pe "s/TAG=.*/TAG=$2/" .env
echo "About to deploy release $2"
echo "gateway"
cd gateway
bash mvnw clean
bash mvnw -ntp -Pprod verify jib:dockerBuild -Drelease.tag=$2 -Dangular.env=$1
echo "user-service"
cd ../user-service
bash mvnw clean
bash mvnw -ntp -Pprod verify jib:dockerBuild -Drelease.tag=$2
echo "assertion-service"
cd ../assertion-service
bash mvnw clean
bash mvnw -ntp -Pprod verify jib:dockerBuild -Drelease.tag=$2
echo "member-service"
cd ../member-service
bash mvnw clean
bash mvnw -ntp -Pprod verify jib:dockerBuild -Drelease.tag=$2
echo "Running docker compose"
cd ../
docker-compose down
docker-compose up -d
docker system prune -f
echo "Done"
