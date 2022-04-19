/// <reference types="cypress" />
import data from '../../fixtures/test-data.json';
import credentials from '../../fixtures/credentials.json';

describe('Add new user', () => {
  it('Add user', function() {
    // enter email
    cy.programmaticSignin(data.member.users.owner.email, credentials.password);
    cy.visit('/ms-user/new');
    // type in invalid email address
    cy.get('#field_email').type(data.invalidEmail);
    // type in name and surname
    cy.get('#field_firstName').type(data.testString);
    cy.get('#field_lastName').type(data.testString);
    // save button should be disabled
    cy.get('#save-entity2')
      .invoke('attr', 'disabled')
      .should('exist');
    // email input field should have warning label
    cy.get('#field_email')
      .should('have.class', 'ng-invalid')
      // enter existing email address
      .clear()
      .type(data.member.users.owner.email);
    cy.get('#save-entity2').click();
    cy.get('.validation-errors').children().should('have.length', 1)
    cy.get('#field_email')
      .clear()
      .type(data.member.users.newUser.email);
    // check "Organization owner"
    cy.get('#field_mainContact').click();
    // save
    cy.get('#save-entity').click();
    cy.get('.alert-success').should('exist');

    // check org owner update email
    const date = new Date()
    cy.checkInbox(data.outbox.ownerUpdateSubject, date);

    // check activation email and follow the provided url
    cy.checkInbox(data.outbox.activationSubject, date).then(email => {
      cy.visitLinkFromEmail(email);
    });


    cy.processPasswordForm('#password');
    // check success message
    cy.get('.alert-success').within(() => {
      cy.get('a')
        .filter('[jhitranslate="global.messages.info.authenticated.link.string"]')
        .click();
    });
    // sign in and confirm the activation was successful
  })

  it('Change organisation owner back to the original user', function() {
    cy.programmaticSignin(data.member.users.newUser.email, credentials.password);
    cy.changeOrgOwner()
  });

  it('Remove added user', function() {
    cy.programmaticSignin(data.member.users.owner.email, credentials.password);
    cy.visit('/ms-user')
    cy.get('.btn-group').each($e => {
      cy.wrap($e).children().last().invoke('attr', 'disabled').then((disabled) => {
        disabled ? cy.log("Skipping user, button is disabled") : cy.removeAffiliation($e)
      })
    })
  });  
});
