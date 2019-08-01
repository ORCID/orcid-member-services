- Prerequisites

Java – tested with java 8, should be tested on java 11
Git
NodeJS
Yeoman
Yarn
MongoDB

- Clone the orcid-member-services repository:

git clone git@github.com:ORCID/orcid-member-services.git


1. Start the JHipster registry:
- Open a new terminal 
- cd orcid-member-services/jhipster-registry/
- Run .\mvnw
- Wait for it to start

2. Start the gateway/
- Open a new terminal 
- cd orcid-member-services/gateway/    
- Run .\mvnw
- Wait for it to start

- Open a new terminal 
- cd orcid-member-services/oauth2-service/  
- Run .\mvnw
- Wait for it to start

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
