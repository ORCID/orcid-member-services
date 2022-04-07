/// <reference types="cypress" />
import data from '../../fixtures/test-data.json';
import credentials from '../../fixtures/credentials.json';

describe('Test sign in form', () => {
  it('Sign in', function() {
    cy.visit(`/`);
    cy.get('#username')
      .clear()
      .type(data.member.users.owner.email);
    cy.get('#password').type(credentials.password);
    cy.get('button')
      .filter('[type="submit"]')
      .click();
    cy.get('#home-logged-message').should('exist');
  });
});
