# Development setup

## Prerequisites

1. Install the following software

    - OpenJDK 11
    - Git
    - NodeJS
    - Yeoman
    - Yarn
    - MongoDB

## Development setup

### Clone the orcid-member-services repository

Create a `git` directory in your home folder, and clone the orcid-member-services project there:
    - mkdir ~/git
    - cd ~/git
    - git clone git@github.com:ORCID/orcid-member-services.git

### Start the JHipster UAA services

[JHipster UAA](https://www.jhipster.tech/using-uaa/) is the service we use to secure our member services, it consists on     three different applications:
    - The JHipster [registry](https://github.com/jhipster/jhipster-registry)
    - The JHipster [gateway](https://www.jhipster.tech/api-gateway/)
    - The JHipster [oauth2-service](https://www.jhipster.tech/using-uaa)
    
So, the first thing we should do is starting the different JHipster services as follows:   

1. Start the JHipster registry:
    - Open a new terminal 
    - cd orcid-member-services/jhipster-registry/
    - Run `bash mvnw`
    - Wait for it to start

2. Start the JHipster gateway:
    - Open a new terminal 
    - cd orcid-member-services/gateway/    
    - Run `bash mvnw`
    - Wait for it to start

3. Start the oauth2-services
    - Open a new terminal 
    - cd orcid-member-services/oauth2-service/  
    - Run `bash mvnw`
    - Wait for it to start

At this point, all required services are up and running, time to start the `user-settings-service`


- Open a new terminal 
- cd orcid-member-services/user-settings-service/
- Run .\mvnw
- Wait for it to start


---
 Create  user test: 
 
 1. Get a token: 
 
 curl -X POST --data "username=admin&password=admin&grant_type=password&scope=openid" http://web_app:changeit@localhost:9999/oauth/token
 
 2. Create the user:
 
 curl -i -H "Accept: application/json" -H "Content-Type:application/json" -H "Authorization: Bearer <TOKEN>" -X POST --data '{"login":"user99996","password":"password123","email":"user99996@test.com","authorities":["ROLE_USER","CONSORTIUM_LEAD"]},"salesforceId":"salesforce-id","parentSalesforceId":"parent-salesforce-id"'  http://localhost:8081/api/member-services-users

3. Result:

You should end up with:
- One user in the Oauth2Service.jhi_user table, so we can login using the UAA.
- One user in the UserSettingsService.member_services_user, that will keep other user information, like salesforce and client ids
