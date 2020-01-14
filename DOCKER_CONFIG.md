## Run with Docker

1. Install [Docker Desktop](https://docs.docker.com/v17.09/engine/installation/#desktop) and [Docker Compose](https://docs.docker.com/compose/install/)

2. Build a Docker image of each application using Jib Maven plugin (ref: https://www.jhipster.tech/docker-compose/#3)

    cd ~/git/orcid-member-services/gateway/ 
    bash mvnw package -Pprod verify jib:dockerBuild

    cd ~/git/orcid-member-services/oauth2-service/ 
    bash mvnw package -Pprod verify jib:dockerBuild

    cd ~/git/orcid-member-services/user-settings-service/ 
    bash mvnw package -Pprod verify jib:dockerBuild

Note: To build without running tests use ```bash mvnw package -Pprod jib:dockerBuild```

3. Start all services using Docker Compose

    cd ~/git/orcid-member-services/docker-compose
    docker-compose up

4. Stop all services using Docker Compose (note: this does not delete the existing images; to delete images use ```docker-compose --rmi all```)

    docker-compose down 

5. List Docker images currently in use by containers managed with Docker Compose
    
    docker-compose images

5. List all existing Docker images on your machine
    
    docker image list

For a complete list of Docker Compose commands, see [Compose command-line reference](https://docs.docker.com/compose/reference/)

