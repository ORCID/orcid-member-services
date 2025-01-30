# ORCID Member Portal

The ORCID Member Portal is a new suite of tools intended to help organizations make the most of their ORCID membership. This application is currently under development and has not yet been released.

The first phase of development includes features that simplify the process of posting affiliation information (employment, education, etc) to researchers’ ORCID records.

Project tasks are managed in Trello:
- Current development tasks: https://trello.com/b/a8Cxpwqe/member-services-current-development
- Release notes: https://trello.com/b/9Xugawlx/member-services-release-notes-2020

# Development setup

## Prerequisites

- [OpenJDK 11](https://openjdk.java.net/install/)
- [Git](https://git-scm.com/downloads)
- [NodeJS](https://nodejs.org/en/download)
- [Yeoman](https://yeoman.io/learning/)
- [Yarn](https://yarnpkg.com/lang/en/docs/install/#mac-stable)
- [MongoDB](https://docs.mongodb.com/manual/installation/)
- [MongoDB compass](https://www.mongodb.com/products/compass) also recommended
- [Angular CLI](https://v16.angular.io/cli)

## Install and start MongoDB

Install and start [MongoDB Community Edition for your OS](https://docs.mongodb.com/manual/administration/install-community/)

## Clone the orcid-member-services repository

Create a `git` directory in your home folder, and clone the orcid-member-services project there:

    mkdir ~/git
    cd ~/git
    git clone git@github.com:ORCID/orcid-member-services.git

## Set Java version to Open JDK 11

Edit bash profile to set JAVA_HOME to your OpenJDK 11 path, ex:

    vim ~/.bash_profile
    export JAVA_HOME=$(/usr/libexec/java_home -v 11)

##

Set up environment variables required by the application:

- APPLICATION_BASEURL : base url of the application, eg https://member-portal.qa.orcid.org for ORCID's QA instance
- APPLICATION_CONTACT_UPDATE_RECIPIENT : email of contact update recipient
- APPLICATION_ENCRYPT_KEY : encryption key
- APPLICATION_ENCRYPT_SALT : encryption salt
- APPLICATION_INTERNAL_ACCESS_TOKEN : access token for internal ORCID endpoints
- APPLICATION_INTERNAL_API_ENDPOINT : base url of internal ORCID endpoint
- APPLICATION_LANDING_PAGE_URL : oauth landing page
- APPLICATION_MAIL_API_KEY : mail api key
- APPLICATION_MAIL_API_URL : mail api url 
- APPLICATION_MAIL_DOMAIN : mail domain
- APPLICATION_MAIL_FROM_ADDRESS : mail from address
- APPLICATION_MAIL_FROM_NAME : mail from name
- APPLICATION_ORCIDAPIENDPOINT : orcid api endpoint
- APPLICATION_RESEND_NOTIFICATION_CRON : cron expression for resending notifications job
- APPLICATION_RESEND_NOTIFICATION_DAYS : time delay in days for resending notifications
- APPLICATION_SALESFORCE_REQUEST_TIMEOUT : salesforce client timeout
- APPLICATION_TOKEN_EXCHANGE_CLIENT_ID : salesforce client id
- APPLICATION_TOKEN_EXCHANGE_CLIENT_SECRET : salesforce client secret
- APPLICATION_TOKEN_EXCHANGE_ENDPOINT : token exchange endpoint for salesforce client
- MEMBER_ASSERTION_STATS_CRON : cron expression for affiliation stats job
- SALESFORCE_CLIENT_ENDPOINT : salesforce client endpoint 
- SALESFORCE_CLIENT_TOKEN : salesforce client token
- STORED_FILE_LIFESPAN : lifespan of stoerd files 
- UAA_KEYSTORE_NAME : keystore file
- UAA_KEYSTORE_PASSWORD : keystore password

## Start the discovery service

- Open a new terminal
- cd orcid-member-services/discovery-service/
- Run `bash mvnw`
- Wait for it to start
- Verify it has started properly. Go to http://localhost:8761/#/ and sign in with `admin`, password `admin`


## Start the user service

Our user service, based on [JHipster UAA](https://www.jhipster.tech/using-uaa/), is the service we use to secure our member services app. We also use it for all user based functionality.

> **IMPORTANT!** For running locally without an email server connected, disable mail health check for oauth2-services before starting. Edit [oauth2-service/src/main/resources/config/application.yml](https://github.com/ORCID/orcid-member-services/blob/master/oauth2-service/src/main/resources/config/application.yml#L60) and set health - mail - enabled to false.

- Open a new terminal
- cd orcid-member-services/user-service/
- Run `bash mvnw`
- Wait for it to start

## Start the gateway:

- Start MongoDB (e.g. `mongod --config /usr/local/etc/mongod.conf --fork`)
- Open a new terminal
- cd orcid-member-services/gateway/
- Run `bash mvnw`
- Wait for it to start
- Go to [http://localhost:8080/](http://localhost:8080/) and sign in with the admin credentials `admin / admin`

## Start the Angular frontend
- Open a new terminal 
- cd orcid-member-services/ui
- Run `ng serve`
- Wait for it to start

## Start the assertion service

- Open a new terminal
- cd orcid-member-services/assertion-service
- Run `bash mvnw`
- Wait for it to start

## Start the member service

- Open a new terminal
- cd orcid-member-services/member-service
- Run `bash mvnw`
- Wait for it to start

## Notes

- As long as the jhipster-registry is running first, the starting order of the other services is not important. They can also be started concurrently.
