echo "Getting latest master"
git checkout master
git pull
echo "Creating tag $1"
git tag $1
git push origin --tags --no-verify
echo "jhipster-registry"
cd jhipster-registry
bash mvnw -ntp -Pdev verify jib:dockerBuild
echo "gateway"
cd ../gateway
bash mvnw -ntp -Pdev verify jib:dockerBuild
echo "oauth2-service"
cd ../oauth2-service
bash mvnw -ntp -Pdev verify jib:dockerBuild
echo "user-settings-service"
cd ../user-settings-service
bash mvnw -ntp -Pdev verify jib:dockerBuild
echo "assertion-services"
cd ../assertion-services
bash mvnw -ntp -Pdev verify jib:dockerBuild
echo "Running docker compose"
cd ../
docker-compose down
docker-compose up -d
echo "Done"