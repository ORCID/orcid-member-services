// ***********************************************
// This example commands.js shows you how to
// create various custom commands and overwrite
// existing commands.
//
// For more comprehensive examples of custom
// commands please read more here:
// https://on.cypress.io/custom-commands
// ***********************************************
//
//
// -- This is a parent command --
// Cypress.Commands.add('login', (email, password) => { ... })
//
//
// -- This is a child command --
// Cypress.Commands.add('drag', { prevSubject: 'element'}, (subject, options) => { ... })
//
//
// -- This is a dual command --
// Cypress.Commands.add('dismiss', { prevSubject: 'optional'}, (subject, options) => { ... })
//
//
// -- This will overwrite an existing command --
// Cypress.Commands.overwrite('visit', (originalFn, url, options) => { ... })
import 'cypress-file-upload';
import data from '../fixtures/test-data.json';
import credentials from '../fixtures/credentials.json';
import record from '../fixtures/orcid-record.json';
  
Cypress.Commands.add('signin', (email, password) => {
  cy.get('#username')
    .clear()
    .type(email)
    .get('#password')
    .type(password)
    .get('button')
    .filter('[type="submit"]')
    .click();
  cy.get('#home-logged-message').should('exist');
});

Cypress.Commands.add('checkOrgId', (org, invalidId, id) => {
  cy.get('#field_disambiguationSource').select(org);
  cy.get('small')
    .filter('[jhitranslate="entity.validation.disambiguatedOrgId.string"]')
    .should('exist');
  cy.get('#field_disambiguatedOrgId')
    .clear()
    .type(invalidId);
  cy.get('small')
    .filter('[jhitranslate="entity.validation.disambiguatedOrgId.string"]')
    .should('exist');
  cy.get('#field_disambiguatedOrgId')
    .clear()
    .type(id);
  cy.get('small').should('not.exist');
});

Cypress.Commands.add('programmaticSignin', (username, password) => {
  cy.getCookie('XSRF-TOKEN').then(csrfCookie => {
    if (!csrfCookie) {
      return cy
        .visit('/')
        .getCookie('XSRF-TOKEN')
        .then(() => cy.programmaticSignin(username, password));
    } else {
      cy.log(csrfCookie.value)
      cy.request({
        method: 'POST',
        url: '/auth/login',
        headers: { 'X-XSRF-TOKEN': csrfCookie.value },
        failOnStatusCode: false, // dont fail so we can make assertions
        body: {
          username,
          password
        }
      }).then(r => {
        cy.log(r);
        expect(r.status).to.eq(200);
      });
    }
  });
});

Cypress.Commands.add('programmaticSignout', () => {
  cy.getCookie('XSRF-TOKEN').then(csrfCookie => {
    cy.log(csrfCookie.value)
    cy.request({
      method: 'POST',
      url: '/auth/logout',
      headers: { 'X-XSRF-TOKEN': csrfCookie.value },
      failOnStatusCode: false // dont fail so we can make assertions
    }).then(r => {
      cy.log(r);  
      // expect(r.status).to.eq(204);
    });
  });
});

Cypress.Commands.add('processPasswordForm', (newPasswordFieldId) => {
  cy.get('button')
    .filter('[type="submit"]')
    // make sure you can't activate account without providing a password
    .invoke('attr', 'disabled')
    .should('exist');
  // type invalid passwords
  cy.get(newPasswordFieldId).clear().type(credentials.shortPassword);
  cy.get('#confirmPassword').clear().type(credentials.shortConfirmationPassword);
  // check for min length error messages
  cy.get('small')
    .filter('[jhitranslate="global.messages.validate.newpassword.minlength.string"]')
    .should('exist');
  cy.get('small')
    .filter('[jhitranslate="global.messages.validate.confirmpassword.minlength.string"]')
    .should('exist');
  // fix password
  cy.get(newPasswordFieldId)
    .clear()
    .type(credentials.password);
  // enter invalid confirmation password
  cy.get('#confirmPassword')
    .clear()
    .type(credentials.wrongConfirmationPasssword);
  // make sure you can't activate account
  cy.get('button')
    .filter('[type="submit"]')
    .click();
  // check for confirmation error message
  cy.get('div')
    .filter('[jhitranslate="global.messages.error.dontmatch.string"]')
    .should('exist');
  // fix confirmation password
  cy.get('#confirmPassword').clear().type(credentials.password);
  // activate account
  cy.get('button')
    .filter('[type="submit"]')
    .click();
});

Cypress.Commands.add('visitLinkFromEmail', (email) => {
  const emailBody = email[0].body.html
  assert.isNotNull(emailBody)
  cy.log('>>>>>>>>>Email body is: ' + JSON.stringify(email.body))
  //convert string to DOM
  const htmlDom = new DOMParser().parseFromString(emailBody, 'text/html')
  //href points to correct endpoint
  const href = htmlDom.querySelector('a[href*="https://member-portal.qa.orcid.org/reset/finish?key="]').href
  cy.visit(href)
});

Cypress.Commands.add('checkInbox', (subject, recipient, date) => {
  cy.task('checkInbox', {
    options: {
      from: data.outbox.email,
      to: recipient,
      subject,
      include_body: true,
      after: date
    }
  })
})

Cypress.Commands.add('removeAffiliation', ($e) => {
  cy.wrap($e).children().last().click()
  cy.get('#jhi-confirm-delete-msUser').click()
})

Cypress.Commands.add('changeOrgOwner', () => {
  cy.visit(`/user/${data.member.users.owner.id}/edit`)
  cy.get("#field_mainContact").click()
  cy.get('#save-entity').click()
  cy.get('.alert-success').should('exist');
  cy.programmaticSignout()
})

Cypress.Commands.add('readCsv', (data) => {
  var lines=data.split("\n");
  var result = [];
  var headers=lines[0].split(",");
  for(var i=1;i<lines.length;i++){

      var obj = {};
      var currentline=lines[i].split(",");

      for(var j=0;j<headers.length;j++){
          obj[headers[j]] = currentline[j];
      }
      result.push(obj);
  }
  return result
})

Cypress.Commands.add('uploadCsv', (path) => {
  cy.get('#jh-upload-entities').click()
  cy.get('#field_filePath').attachFile(path)
  cy.get('#jhi-confirm-csv-upload').click()
})

Cypress.Commands.add('fetchLinkAndGrantPermission', () => {
  // get perimssion link from first affiliation in the list
  cy.get('tbody')
    .children()
    .last()
    .within(() => {
      cy.get('a')
        .filter('[jhitranslate="gatewayApp.assertionServiceAssertion.details.string"]')
        .click();
    });
  cy.get('.jh-entity-details').within(() =>
  cy
    .get('button')
    .filter('[jhitranslate="gatewayApp.assertionServiceAssertion.copyClipboard.string"]')
    .click()
  );
  cy.task('getClipboard').then(link => {
    cy.visit(link);
  });
  // Grant permission
  cy.get('#username')
    .clear()
    .type(record.email);
  cy.get('#password').type(credentials.password);
  cy.get('#signin-button').click();

  // *ADD ID
  cy.get('.mt-5').within(() => {
    cy.get('h2')
      .filter('[jhitranslate="landingPage.success.thanks.string"]')
      .should('exist');
  });
})

Cypress.Commands.add('checkAffiliationChanges', (affiliation, value) => {
  expect(affiliation['department-name']).to.eq(value);
  expect(affiliation['role-title']).to.eq(value);
  expect(affiliation['organization']['address']['city']).to.eq(value);
  expect(affiliation['organization']['name']).to.eq(value);
})
