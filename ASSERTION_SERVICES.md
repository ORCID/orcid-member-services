# Assertion services

## Prerequisites

Configure the orcid-mermber-services JHipster UAA services, as explained [here](README.md).

## Start the assertion-services

The `assertion-services` is the service that will allow us to CUD assertions (affiliations) in ORCID records, using this microservice, you will be able to create, update or delete assertions associated with specific ORCID records.

To start the `assertion-services`:

- Open a new terminal 
- cd orcid-member-services/assertion-services/
- Run `bash mvnw`
- Wait for it to start

## Test it

To test that the `assertion-services` is working as expected, we will go through the process of uploading an affiliation and wait until it get pushed to an ORCID record.

#### Create a new user

We will access the `assertion-services` functionality through the JHipster gateway, and, to be able to use it, we will need a user with the `ASSERTION_SERVICE_ENABLED` authority enabled, so, lets create a new user:

- Generate an access token as explained in [Create a user](#Create-a-user) section
- Use that token to create a user with the `ASSERTION_SERVICE_ENABLED` authority enabled:
```
curl -i -H "Accept: application/json" -H "Content-Type:application/json" -H "Authorization: Bearer <TOKEN>" -X POST --data '{"login":"test_user_1","email":"test_user_1@test.com", "firstName":"Angel", "lastName":"Montenegro", "password":"password123","authorities":["ROLE_USER","ASSERTION_SERVICE_ENABLED"],"salesforceId":"SF1", "parentSalesforceId":"PSF1"}'  http://localhost:8081/settings/api/user

```

#### Login to the gateway app

#### Create an assertions file

#### Upload the assertions file

#### Review the existing assertions
