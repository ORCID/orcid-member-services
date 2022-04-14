/// <reference types="cypress" />
import data from '../../fixtures/test-data.json';
import credentials from '../../fixtures/credentials.json';

describe('Test the password reset functionality', () => {
  it('Reset password', function () {
    cy.visit('/reset/request')
    cy.get('#email').type(data.invalidEmail)
    cy.get('small').filter('[jhitranslate="global.messages.validate.email.invalid.string"]').should('exist')
    cy.get('button').filter('[type="submit"]').invoke('attr', 'disabled').should('exist')
    cy.get('#email').clear().type(data.member.users.owner.email)
    cy.get('button').filter('[type="submit"]').click()

    cy.task('checkInbox', {
      options: {
        from: data.outbox.email,
        to: data.member.users.owner.email,
        subject: data.outbox.resetPasswordSubject,
        include_body: true,
        after: Date.now()
      },
    }).then((email) => {
      cy.visitLinkFromEmail(email)
    })
    cy.processPasswordForm()

    cy.get(".alert-success").within(() => {cy.get("a").filter('[jhitranslate="global.messages.info.authenticated.link.string"]').click()})
    // sign in and confirm the activation was successful
    cy.signin(data.member.users.owner.email, credentials.password)
    
  })
})
