echo "Getting latest master"
git checkout master
git pull
echo "Creating tag $1"
git tag $1
git push origin --tags --no-verify
echo "jhipster-registry"
cd jhipster-registry
bash mvnw -ntp -Pdev clean verify jib:dockerBuild -Drelease.tag=$1
echo "gateway"
cd ../gateway
bash mvnw -ntp -Pdev clean verify jib:dockerBuild -Drelease.tag=$1
echo "oauth2-service"
cd ../oauth2-service
bash mvnw -ntp -Pdev clean verify jib:dockerBuild -Drelease.tag=$1
echo "user-settings-service"
cd ../user-settings-service
bash mvnw -ntp -Pdev clean verify jib:dockerBuild -Drelease.tag=$1
echo "assertion-services"
cd ../assertion-services
bash mvnw -ntp -Pdev clean verify jib:dockerBuild -Drelease.tag=$1
echo "Running docker compose"
cd ../
docker-compose down
export TAG=$1
docker-compose up -d
echo "Done"