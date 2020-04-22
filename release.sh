if [ "$1" != "current" ]
then
    echo "Getting latest master"
    git checkout master
    git pull
    if [ "$2" = "tagRelease" ]
    then
        echo "Creating tag $1"
        git tag $1
        git push origin --tags --no-verify
    else
        git checkout $1
    fi
fi
echo "About to deploy release $1"
echo "gateway"
cd ../gateway
bash mvnw clean
bash mvnw -ntp -Pdev verify jib:dockerBuild -Drelease.tag=$1
echo "oauth2-service"
cd ../oauth2-service
bash mvnw clean
bash mvnw -ntp -Pdev verify jib:dockerBuild -Drelease.tag=$1
echo "user-settings-service"
cd ../user-settings-service
bash mvnw clean
bash mvnw -ntp -Pdev verify jib:dockerBuild -Drelease.tag=$1
echo "assertion-services"
cd ../assertion-services
bash mvnw clean
bash mvnw -ntp -Pdev verify jib:dockerBuild -Drelease.tag=$1
echo "Running docker compose"
cd ../
export TAG=$1
docker-compose down
docker system prune -f
docker-compose up -d
echo "Done"