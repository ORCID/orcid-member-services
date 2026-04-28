# ORCID Member Portal

The ORCID Member Portal is a new suite of tools intended to help organizations make the most of their ORCID membership. This application is currently under development and has not yet been released.

The first phase of development includes features that simplify the process of posting affiliation information (employment, education, etc) to researchers’ ORCID records.

Project tasks are managed in Trello:
- Current development tasks: https://trello.com/b/a8Cxpwqe/member-services-current-development
- Release notes: https://trello.com/b/9Xugawlx/member-services-release-notes-2020

## Development setup

### Prerequisites

- [OpenJDK 11](https://openjdk.java.net/install/)
- [Git](https://git-scm.com/downloads)
- [NodeJS](https://nodejs.org/en/download)
- [Yeoman](https://yeoman.io/learning/)
- [Yarn](https://yarnpkg.com/lang/en/docs/install/#mac-stable)
- [MongoDB](https://docs.mongodb.com/manual/installation/)
- [MongoDB compass](https://www.mongodb.com/products/compass) also recommended
- [Angular CLI](https://v16.angular.io/cli)

### Environment configuration

Use the provided `.env.example` as the starting point for local configuration.

Copy it to a local `.env` file and update the values for your environment. Keep secrets out of version control.

### Install and start MongoDB

Install and start [MongoDB Community Edition for your OS](https://docs.mongodb.com/manual/administration/install-community/)

### Clone the repository

Create a `git` directory in your home folder, and clone the orcid-member-services project there:

```bash
    mkdir ~/git
    cd ~/git
    git clone git@github.com:ORCID/orcid-member-services.git
```

### Set Java version to Open JDK 11

Edit bash profile to set JAVA_HOME to your OpenJDK 11 path, ex:

```bash
    vim ~/.bash_profile
    export JAVA_HOME=$(/usr/libexec/java_home -v 11)
```

### Start the user service

> **IMPORTANT!** For running locally without an email server connected, disable mail health check for oauth2-services before starting. Edit [oauth2-service/src/main/resources/config/application.yml](https://github.com/ORCID/orcid-member-services/blob/master/oauth2-service/src/main/resources/config/application.yml#L60) and set health - mail - enabled to false.

```bash
cd user-service-2
./mvnw
```

### Start the Angular frontend

```bash
cd ui-2
ng serve
```

### Start the assertion service

```bash
cd assertion-service-2
./mvnw
```

### Start the member service

```bash
cd member-service-2
./mvnw
```

## Docker-based setup

The project can also be run with Docker.

### 1. Prepare environment variables

Start from `.env.example`, copy it to `.env`, and fill in the values required for your environment.

### 2. Build and start the stack

Use the provided script to build the Java services and start the containers:

```bash
./docker-build.sh
```

This script:
- builds the Java service JARs
- runs `docker compose build`
- starts the containers in detached mode

### 3. Verify the services

After startup, check that the services are available through your configured Docker setup.

## Notes

- Keep `.env` local and uncommitted.
- `.env.example` should remain a safe template for new contributors.