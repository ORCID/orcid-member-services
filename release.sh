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

if [[ -z "$DOCKER_REG" ]];then
  echo "please set DOCKER_REG environment variable"
  exit
fi

echo "about to build docker images with release $2"
echo "gateway"
cd gateway
bash mvnw clean
if [ "$1" != "all" ]
then
    echo "building gateway image for $1"
    if [ "$1" = "sbox" ]
    then
        bash mvnw -ntp -Pprod verify jib:dockerBuild -Drelease.tag=$2 -DDOCKER_REG=$DOCKER_REG -Dangular.env=sandbox
    else
        echo "building gateway image for $1"
        bash mvnw -ntp -Pprod verify jib:dockerBuild -Drelease.tag=$2 -DDOCKER_REG=$DOCKER_REG -Dangular.env=$1
    fi
    echo "pushing gateway image for $1 to nexus"
    docker push ${DOCKER_REG}/gateway:$2-$1
else
    echo "building gateway image for qa"
    bash mvnw -ntp -Pprod verify jib:dockerBuild -Drelease.tag=$2 -DDOCKER_REG=$DOCKER_REG -Dangular.env=qa
    echo "pushing gateway image for qa to nexus"
    docker push ${DOCKER_REG}/gateway:$2-qa
    echo "building gateway image for sandbox"
    bash mvnw -ntp -Pprod verify jib:dockerBuild -Drelease.tag=$2 -DDOCKER_REG=$DOCKER_REG -Dangular.env=sandbox
    echo "pushing gateway image for sandbox to nexus"
    docker push ${DOCKER_REG}/gateway:$2-sandbox
    echo "building gateway image for prod"
    bash mvnw -ntp -Pprod verify jib:dockerBuild -Drelease.tag=$2 -DDOCKER_REG=$DOCKER_REG -Dangular.env=prod
    echo "pushing gateway image for prod to nexus"
    docker push ${DOCKER_REG}/gateway:$2-prod
fi
echo "userservice"
cd ../user-service
bash mvnw clean
bash mvnw -ntp -Pprod verify jib:dockerBuild -Drelease.tag=$2 -DDOCKER_REG=$DOCKER_REG
echo "pushing userservice image to nexus"
docker push ${DOCKER_REG}/userservice:$2
echo "assertionservice"
cd ../assertion-service
bash mvnw clean
bash mvnw -ntp -Pprod verify jib:dockerBuild -Drelease.tag=$2 -DDOCKER_REG=$DOCKER_REG
echo "pushing assertionservice image to nexus"
docker push ${DOCKER_REG}/assertionservice:$2
echo "memberservice"
cd ../member-service
bash mvnw clean
bash mvnw -ntp -Pprod verify jib:dockerBuild -Drelease.tag=$2 -DDOCKER_REG=$DOCKER_REG
echo "pushing memberservice image to nexus"
docker push ${DOCKER_REG}/memberservice:$2
echo "Done"
