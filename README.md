ORCID Member Services is a new suite of tools intended to help organizations make the most of their ORCID membership. This application is currently under development and has not yet been released.

The first phase of development includes features that simplify the process of posting affiliation information (employment, education, etc) to researchers’ ORCID records. 

Project tasks are managed in Trello:
- Current development tasks: https://trello.com/b/a8Cxpwqe/member-services-current-development
- Release notes: https://trello.com/b/9Xugawlx/member-services-release-notes-2020

# Development setup

## Prerequisites

1. Install the following software

    - [OpenJDK 11](https://openjdk.java.net/install/)
    - [Git](https://git-scm.com/downloads)
    - [NodeJS](https://nodejs.org/en/download)
    - [Yeoman](https://yeoman.io/learning/)
    - [Yarn](https://yarnpkg.com/lang/en/docs/install/#mac-stable)
    - [MongoDB](https://docs.mongodb.com/manual/installation/)
    - [MongoDB compass](https://www.mongodb.com/products/compass) also recommended

## Install and start MongoDB

Install and start [MongoDB Community Edition for your OS](https://docs.mongodb.com/manual/administration/install-community/)

## Clone the orcid-member-services repository

Create a `git` directory in your home folder, and clone the orcid-member-services project there:

- mkdir ~/git
- cd ~/git
- git clone git@github.com:ORCID/orcid-member-services.git

## Set Java version to Open JDK 11

Edit bash profile to set JAVA_HOME to your OpenJDK 11 path, ex:

        vim ~/.bash_profile
        export JAVA_HOME=$(/usr/libexec/java_home -v 11)

**IMPORTANT!!!** You will need to set JAVA_HOME back to Java 8 in order to work on ORCID-Source  

## Start the JHipster registry

    - Open a new terminal 
    - cd orcid-member-services/jhipster-registry/
    - Run `bash start.sh`
    - Wait for it to start
    - Go to [http://localhost:8761/#/](http://localhost:8761/#/) and sign in with the admin credentials `admin@orcid.org / admin`


## Start the other services  

1. Start the user-service

Our user service, based on [JHipster UAA](https://www.jhipster.tech/using-uaa/), is the service we use to secure our member services app. We also use it for all user based functionality. 

    - **IMPORTANT!** For running locally without an email server connected, disable mail health check for oauth2-services before starting. Edit [oauth2-service/src/main/resources/config/application.yml](https://github.com/ORCID/orcid-member-services/blob/master/oauth2-service/src/main/resources/config/application.yml#L60)
    - Set 
        ```
        health:
            mail:
               enabled: false
        ```
    - Open a new terminal 
    - cd orcid-member-services/user-service/  
    - Run `bash mvnw`
    - Wait for it to start

2. Start the JHipster gateway:
    
    - Start MongoDB
    - Open a new terminal 
    - cd orcid-member-services/gateway/    
    - Run `bash mvnw`
    - Wait for it to start
    - Go to [http://localhost:8080/](http://localhost:8080/) and sign in with the admin credentials `admin / admin`
    
3. Start the Angular frontend (only required after making front end changes)

    - Open a new terminal 
    - cd orcid-member-services/gateway 
    - Run `npm install` then `npm start`
    - Wait for it to start
    - Optionally kill it and start up again using bash mvnw (like the other services)
    
4. Start the assertion service

    - Open a new terminal 
    - cd orcid-member-services/assertion-service
    - Run `bash mvnw`
    - Wait for it to start

5. Start the member service

    - Open a new terminal 
    - cd orcid-member-services/member-service
    - Run `bash mvnw`
    - Wait for it to start

## Start Jhipster Console

[Jhipster Console](https://github.com/jhipster/jhipster-console) provides and ELK stack logging and metrics UI. To run Jhipster Console locally:
    
    - In each microservice, edit src/main/resources/config/application-dev.yml to set 
            logstash:
                enabled: true
                
    - Start Docker desktop
    - Open a new terminal 
    - cd orcid-member-services/jhipster-console/
    - Run ```docker-compose up```
    - Wait for it to start
    - Go to [http://localhost:5601](http://localhost:5601) to see Kibana logging dashboard

## Deployment via Docker Compose
Member services is deployed to AWS using Docker Compose.
See [Member services wiki page](https://github.com/ORCID/ORCID-Internal/wiki/Member-services)
