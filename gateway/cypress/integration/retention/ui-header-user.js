/// <reference types="cypress" />
import data from '../../fixtures/test-data.json';
import credentials from '../../fixtures/credentials.json';

describe('Test header for appropriate drop down menus', () => {
  afterEach(() => {
    cy.programmaticSignout();
  })

  it('Test user account', function() {
    cy.programmaticSignin(data.populatedMember.users.user.email, credentials.password);
    cy.visit('/');
    cy.get('#admin-menu').should('not.exist');
    cy.get('#entity-menu').should('exist')
  });

  it('Test owner account', function() {
    cy.programmaticSignin(data.populatedMember.users.owner.email, credentials.password);
    cy.visit('/');
    cy.get('#admin-menu').should('exist');
    cy.get('#entity-menu').should('exist');
    cy.get('a').filter('[href="/user"]').should('exist');
    cy.get('a').filter('[href="/member"]').should('not.exist');
  });

  it('Test admin account', function() {
    cy.programmaticSignin(credentials.adminEmail, credentials.adminPassword);
    cy.visit('/');
    cy.get('#admin-menu').should('exist');
    cy.get('#entity-menu').should('exist')
    cy.get('a').filter('[href="/user"]').should('exist');
    cy.get('a').filter('[href="/member"]').should('exist');
  });
});
