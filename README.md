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

### Start the user-settings-service

The `user-settings-service` is the user management micro service, it will keep information of user roles and will allow users to perform administrative stuff like resetting their password.

To start the `user-settings-service`:

- Open a new terminal 
- cd orcid-member-services/user-settings-service/
- Run `bash mvnw`
- Wait for it to start

#### Test it is working as expected

To test that the `user-settings-service` is working as expected, we will create a user through it and then retrive the user information.

1. First we need to generate an access token using the default admin user credentials `admin / admin`:

    curl -X POST --data "username=admin&password=admin&grant_type=password&scope=openid" http://web_app:changeit@localhost:9999/oauth/token

    This will return an access token that looks like this one: 
    
```json
{
  "access_token" : "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX25hbWUiOiJhZG1pbiIsInNjb3BlIjpbIm9wZW5pZCJdLCJleHAiOjE1NzI5MDM3NTUsImlhdCI6MTU3MjkwMzQ1NSwiYXV0aG9yaXRpZXMiOlsiUk9MRV9BRE1JTiIsIlJPTEVfVVNFUiJdLCJqdGkiOiJmMTY2NDU0OS1kNjMwLTRmZjEtODFiOC04OTk1YmY1NDcyNDQiLCJjbGllbnRfaWQiOiJ3ZWJfYXBwIn0.RIWhnROHVn5xAy3ChrqpclEtsX8E_rAA_r02LVtfMA-k9PS0N_kL5Sn466HBbAKwQQDFQYMDnI6uhU0eb1lEy9E20fCDPW5m8hJ-anKc7hWKCOxoPzj4-gtM4BCriKANHlRJy4168xUaA99NnZn2R3XBSOLMpnxNt7RrrwmDs-8qjN0qvcZGHCjqa7o1g5kRWskMPt6bq8WwxhPtdJh59b7MSsXyn-gfHqPLzcmi8ImlZWjRdMMaxEr_k19gpDWjF1h2aghtuCyqyVl8tN9NXQtBNSAOUSF9Zwxhrg9msbzZq9hsQLTYuUWcyxdJ3Pb_PZcxeosMCo5XzJcxQ8V9Jg",
  "token_type" : "bearer",
  "refresh_token" : "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX25hbWUiOiJhZG1pbiIsInNjb3BlIjpbIm9wZW5pZCJdLCJhdGkiOiJmMTY2NDU0OS1kNjMwLTRmZjEtODFiOC04OTk1YmY1NDcyNDQiLCJleHAiOjE1NzM1MDgyNTUsImlhdCI6MTU3MjkwMzQ1NSwiYXV0aG9yaXRpZXMiOlsiUk9MRV9BRE1JTiIsIlJPTEVfVVNFUiJdLCJqdGkiOiJhOTNjZTI2ZC03NmYwLTRkMTYtYjU1Mi1lZDhlNTMyMDdhNmYiLCJjbGllbnRfaWQiOiJ3ZWJfYXBwIn0.M8ts92_VHtukPbYMDxCk1xVP2_pCgnKPI6WmZPuHUBBZv1XyfC5PqXowDjQlUwF1r3SOVYcuCKXrwm8kAzhVnRW0BnHK-BvyUDMzZ9B19J0jfbvk_X2VGwB_k1JNbVnOrN-r1wdMnmqD0lF74J6Ef04_iMdpgotbiPKyLrUtUXIivDPjJYIBIDLGeNGeg3ka7uVt9YHdcFa-wBBxR9r2DeIIgsZX3NDuGiT2xfqWBQhfceR4zyb3DCOhYYUokGh719HkBWlp-pv3sxFL2sTZbjGaSIpebPW-HMwbQoko1e1y1nCJQ0ci3cZmhZ-QUYFm7BwvHoIZsgiG-4whTH7lIA",
  "expires_in" : 299,
  "scope" : "openid",
  "iat" : 1572903455,
  "jti" : "f1664549-d630-4ff1-81b8-8995bf547244"
}
```

From it, we just need to get the value of the `access_token` parameter.
    

---
 Create  user test: 
 
 1. Get a token: 
 
    curl -X POST --data "username=admin&password=admin&grant_type=password&scope=openid" http://web_app:changeit@localhost:9999/oauth/token
 
    This will  
 
 2. Create the user:
 
 curl -i -H "Accept: application/json" -H "Content-Type:application/json" -H "Authorization: Bearer <TOKEN>" -X POST --data '{"login":"user99996","password":"password123","email":"user99996@test.com","authorities":["ROLE_USER","CONSORTIUM_LEAD"]},"salesforceId":"salesforce-id","parentSalesforceId":"parent-salesforce-id"'  http://localhost:8081/api/member-services-users

3. Result:

You should end up with:
- One user in the Oauth2Service.jhi_user table, so we can login using the UAA.
- One user in the UserSettingsService.member_services_user, that will keep other user information, like salesforce and client ids
