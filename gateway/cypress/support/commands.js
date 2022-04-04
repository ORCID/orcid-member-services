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
import data from '../fixtures/test-data.json';
import credentials from '../fixtures/credentials.json';

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
    cy.request({
      method: 'POST',
      url: '/auth/logout',
      headers: { 'X-XSRF-TOKEN': csrfCookie.value },
      failOnStatusCode: false // dont fail so we can make assertions
    }).then(r => {
      cy.log(r);
      expect(r.status).to.eq(204);
    });
  });
});

Cypress.Commands.add('processPasswordForm', () => {
  cy.get('button')
    .filter('[type="submit"]')
    // make sure you can't activate account without providing a password
    .invoke('attr', 'disabled')
    .should('exist');
  // type invalid passwords
  cy.get('#password').type(credentials.shortPassword);
  cy.get('#confirmPassword').type(credentials.shortConfirmationPassword);
  // check for min length error messages
  cy.get('small')
    .filter('[jhitranslate="global.messages.validate.newpassword.minlength.string"]')
    .should('exist');
  cy.get('small')
    .filter('[jhitranslate="global.messages.validate.confirmpassword.minlength.string"]')
    .should('exist');
  // fix password
  cy.get('#password')
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
  assert.isNotNull(email)
  const emailBody = email.body.html
  cy.log('>>>>>>>>>Email body is: ' + JSON.stringify(email.body))
  //convert string to DOM
  const htmlDom = new DOMParser().parseFromString(emailBody, 'text/html')
  //href points to correct endpoint
  const href = htmlDom.querySelector('a[href*="https://member-portal.qa.orcid.org/reset/finish?key="]').href
  cy.visit(href)
});

Cypress.Commands.add('checkInbox', (subject, date) => {
  cy.task('checkInbox', {
    options: {
      from: data.outbox.email,
      to: data.member.users.newUser.email,
      subject: data.outbox.activationSubject,
      include_body: true,
      after: date
    }
  })
})

Cypress.Commands.add('removeAffiliation', ($e) => {
  cy.wrap($e).children().last().click()
  cy.get('#jhi-confirm-delete-msUser').click()
})

