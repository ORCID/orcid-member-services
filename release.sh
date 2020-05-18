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
cd gateway
bash mvnw clean
bash mvnw -ntp -Pdev verify jib:dockerBuild -Drelease.tag=$1
echo "user-service"
cd ../user-service
bash mvnw clean
bash mvnw -ntp -Pdev verify jib:dockerBuild -Drelease.tag=$1
echo "assertion-service"
cd ../assertion-service
bash mvnw clean
bash mvnw -ntp -Pdev verify jib:dockerBuild -Drelease.tag=$1
echo "member-service"
cd ../member-service
bash mvnw clean
bash mvnw -ntp -Pdev verify jib:dockerBuild -Drelease.tag=$1
echo "Running docker compose"
cd ../
export TAG=$1
docker-compose down
docker system prune -f
docker-compose up -d
echo "Done"
