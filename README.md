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

## Start the JHipster registry

- Open a new terminal
- cd orcid-member-services/jhipster-registry/
- Run `bash start.sh`. This should download the jhipster-registry jar if you don't have it installed. If for some reason the download fails you will need to download it manually
- Wait for the jhipster-registry to start
- Verify it has started properly. Go to http://localhost:8761/#/ and sign in with `admin`, password `admin`


## Start the user service

Our user service, based on [JHipster UAA](https://www.jhipster.tech/using-uaa/), is the service we use to secure our member services app. We also use it for all user based functionality.

> **IMPORTANT!** For running locally without an email server connected, disable mail health check for oauth2-services before starting. Edit [oauth2-service/src/main/resources/config/application.yml](https://github.com/ORCID/orcid-member-services/blob/master/oauth2-service/src/main/resources/config/application.yml#L60) and set health - mail - enabled to false.

- Open a new terminal
- cd orcid-member-services/user-service/
- Run `bash mvnw`
- Wait for it to start

## Start the JHipster gateway:

- Start MongoDB (e.g. `mongod --config /usr/local/etc/mongod.conf --fork`)
- Open a new terminal
- cd orcid-member-services/gateway/
- Run `bash mvnw`
- Wait for it to start
- Go to [http://localhost:8080/](http://localhost:8080/) and sign in with the admin credentials `admin / admin`

## Start the Angular frontend
> This is only required after making front end changes

- Stop the jhipster gateway if it's running
- From the jhiptster gateway base directory, run `npm install` then `npm start`
- Wait for it to start - localhost:9000 will open in a new browser tab once the server is up
- Optionally kill it and start up again using bash mvnw as above

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

## Optional pre-commit

pre-commit is a framework for managing git pre-commit hooks that run before you push code out to do things like linting and syntax checking. To set it up for this project run ./pre-commit-env-setup.sh . This is only tested on amd64 macos. See .pre-commit-config.yaml for the hooks that have been configured.


