# Development setup

## Prerequisites

1. Install the following software

    - [OpenJDK 11](https://openjdk.java.net/install/)
    - [Git](https://git-scm.com/downloads)
    - [NodeJS](https://nodejs.org/en/download)
    - [Yeoman](https://yeoman.io/learning/)
    - [Yarn](https://yarnpkg.com/lang/en/docs/install/#mac-stable)
    - [MongoDB](https://docs.mongodb.com/manual/installation/)

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
    - Go to (http://localhost:8761/#/)[http://localhost:8761/#/] and sign in with the admin credentials `admin / admin`

2. Start the JHipster gateway:
    
    - Start MongoDB
    - Open a new terminal 
    - cd orcid-member-services/gateway/    
    - Run `bash mvnw`
    - Wait for it to start
    - Go to (http://localhost:8080/)[http://localhost:8080/] and sing in with the admin credentials `admin / admin`

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

````
curl -X POST --data "username=admin&password=admin&grant_type=password&scope=openid" http://web_app:changeit@localhost:9999/oauth/token
````

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

From it, take the `access_token` parameter and use it to create a new user, as follows:

```
curl -i -H "Accept: application/json" -H "Content-Type:application/json" -H "Authorization: Bearer <TOKEN>" -X POST --data '{"login":"test_user_1","password":"password123","email":"test_user_1@test.com","authorities":["ROLE_USER","CONSORTIUM_LEAD"]},"salesforceId":"salesforce-id","parentSalesforceId":"parent-salesforce-id"'  http://localhost:8081/api/member-services-users
```

From it, notice the user parameters: 

- login: The name used to login
- password: The user password
- email: The user email
- authorities: The authorities the user should have, by default, they should be `ROLE_USER` and `CONSORTIUM_LEAD`.
- salesforceId: TBD
- parentSalesforceId: TBD

After that, please go to the (gateway app)[http://localhost:8080/], logout if you are not, and, now login using the new credentials you just created.

Then, if you want to explore the database entries created by this call, you can login to you database (MongoDB by default), and, you will see the following changes: 

- In the `Oauth2Service` collection, in the `jhi_user` table, you will see a new user, which is the one used by the UAA to allow you to login.
- In the `UserSettingsService` collection, in the `member_services_user` table, there will be a new user as well, and, on it, the `user_id` field must match the id of the new user in the `Oauth2Service`.`jhi_user` table

If all that is correct, then, the setup is done.
