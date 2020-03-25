# Development setup

## Prerequisites

1. Install the following software

    - [OpenJDK 11](https://openjdk.java.net/install/)
    - [Git](https://git-scm.com/downloads)
    - [NodeJS](https://nodejs.org/en/download)
    - [Yeoman](https://yeoman.io/learning/)
    - [Yarn](https://yarnpkg.com/lang/en/docs/install/#mac-stable)
    - [MongoDB](https://docs.mongodb.com/manual/installation/)

## Install and start MongoDB

Install and start [MongoDB Community Edition for your OS](https://docs.mongodb.com/manual/administration/install-community/)

## Clone the orcid-member-services repository

Create a `git` directory in your home folder, and clone the orcid-member-services project there:

- mkdir ~/git
- cd ~/git
- git clone git@github.com:ORCID/orcid-member-services.git

## Start the JHipster UAA services

[JHipster UAA](https://www.jhipster.tech/using-uaa/) is the service we use to secure our member services app. It serves as the base for user account management in our custom microservices and consists of 3 different applications:

- The JHipster [registry](https://github.com/jhipster/jhipster-registry)
- The JHipster [gateway](https://www.jhipster.tech/api-gateway/)
- The JHipster [oauth2-service](https://www.jhipster.tech/using-uaa)
    
So, the first thing we should do is starting the different JHipster services as follows:   

1. Start the JHipster registry:

    - Open a new terminal 
    - cd orcid-member-services/jhipster-registry/
    - Run `bash mvnw`
    - Wait for it to start
    - Go to [http://localhost:8761/#/](http://localhost:8761/#/) and sign in with the admin credentials `admin / admin`

2. Start the JHipster gateway:
    
    - Start MongoDB
    - Open a new terminal 
    - cd orcid-member-services/gateway/    
    - Run `bash mvnw`
    - Wait for it to start
    - Go to [http://localhost:8080/](http://localhost:8080/) and sing in with the admin credentials `admin / admin`

3. Start the oauth2-services
    For running locally without an email server connected, disable mail health check for oauth2-services before starting. 
    - Edit [oauth2-service/src/main/resources/config/application.yml](https://github.com/ORCID/orcid-member-services/blob/master/oauth2-service/src/main/resources/config/application.yml#L60)
    - Set 
        ```
        health:
            mail:
               enabled: false
        ```
    - Open a new terminal 
    - cd orcid-member-services/oauth2-service/  
    - Run `bash mvnw`
    - Wait for it to start
    
4. Start the Angular frontend

    - Open a new terminal 
    - cd orcid-member-services/gateway 
    - Run `npm install` then `npm start`
    - Wait for it to start

## Start the custom microservices
With UAA up and running, we can now start the custom microservices that depend on it:

- [User settings service](USER_SETTINGS_SERVICE.md)
- [Assertion services](ASSERTION_SERVICES.md)

## Docker Compose configuration
[IN PROGRESS AND KINDA WORKS BUT NOT THAT WELL] [Docker Compose for local development](DOCKER_CONFIG.md)
