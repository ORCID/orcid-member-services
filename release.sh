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

echo "about to build docker images with release $2"
echo "gateway"
cd gateway
bash mvnw clean
if [ "$1" != "all" ]
then
    echo "building gateway image for $1"
    bash mvnw -ntp -Pprod verify jib:dockerBuild -Drelease.tag=$2 -Dangular.env=$1
    echo "pushing gateway image for $1 to nexus"
    docker push dockerpush.int.orcid.org/gateway:$2-$1
else
    echo "building gateway image for qa"
    bash mvnw -ntp -Pprod verify jib:dockerBuild -Drelease.tag=$2 -Dangular.env=qa
    echo "pushing gateway image for qa to nexus"
    docker push dockerpush.int.orcid.org/gateway:$2-qa
    echo "building gateway image for sandbox"
    bash mvnw -ntp -Pprod verify jib:dockerBuild -Drelease.tag=$2 -Dangular.env=sandbox
    echo "pushing gateway image for sandbox to nexus"
    docker push dockerpush.int.orcid.org/gateway:$2-sandbox
    echo "building gateway image for prod"
    bash mvnw -ntp -Pprod verify jib:dockerBuild -Drelease.tag=$2 -Dangular.env=prod
    echo "pushing gateway image for prod to nexus"
    docker push dockerpush.int.orcid.org/gateway:$2-prod
fi
echo "userservice"
cd ../user-service
bash mvnw clean
bash mvnw -ntp -Pprod verify jib:dockerBuild -Drelease.tag=$2
echo "pushing userservice image to nexus"
docker push dockerpush.int.orcid.org/userservice:$2
echo "assertionservice"
cd ../assertion-service
bash mvnw clean
bash mvnw -ntp -Pprod verify jib:dockerBuild -Drelease.tag=$2
echo "pushing assertionservice image to nexus"
docker push dockerpush.int.orcid.org/assertionservice:$2
echo "memberservice"
cd ../member-service
bash mvnw clean
bash mvnw -ntp -Pprod verify jib:dockerBuild -Drelease.tag=$2
echo "pushing memberservice image to nexus"
docker push dockerpush.int.orcid.org/memberservice:$2
echo "Done"
