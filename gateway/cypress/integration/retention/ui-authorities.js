/// <reference types="cypress" />
import data from '../../fixtures/test-data.json';
import credentials from '../../fixtures/credentials.json';

describe('Test authorities', () => {
  afterEach(() => {
    cy.programmaticSignout();
  })

  it('User', function() {
    cy.programmaticSignin(data.populatedMember.users.user.email, credentials.password);
    cy.visit('/');
    cy.get('#admin-menu').should('not.exist');
    cy.get('#entity-menu').should('exist')
    cy.get('a').filter('[href="/assertion"]').should('exist');
    cy.get('a').filter('[href="/user"]').should('not.exist');
    cy.get('a').filter('[href="/member"]').should('not.exist');
  });

  it('Org owner', function() {
    cy.programmaticSignin(data.populatedMember.users.owner.email, credentials.password);
    cy.visit('/');
    cy.get('#admin-menu').should('exist');
    cy.get('#entity-menu').should('exist');
    cy.get('a').filter('[href="/user"]').should('exist');
    cy.get('a').filter('[href="/assertion"]').should('exist');
    cy.get('a').filter('[href="/member"]').should('not.exist');
  });

  it('Admin', function() {
    cy.programmaticSignin(credentials.adminEmail, credentials.adminPassword);
    cy.visit('/');
    cy.get('#admin-menu').should('exist');
    cy.get('#entity-menu').should('exist')
    cy.get('a').filter('[href="/user"]').should('exist');
    cy.get('a').filter('[href="/assertion"]').should('exist');
    cy.get('a').filter('[href="/member"]').should('exist');
  });

  it('Consortium lead', function() {
    cy.programmaticSignin(data.homepageTestMembers.consortiumLeadAndMemberEmail, credentials.password);
    cy.visit('/');
    cy.get('#admin-menu').should('exist');
    cy.get('#entity-menu').should('exist')
    cy.get('a').filter('[href="/user"]').should('exist');
    cy.get('a').filter('[href="/assertion"]').should('not.exist');
    cy.get('a').filter('[href="/member"]').should('not.exist');

  });

  it('Anonymous', function() {
    cy.programmaticSignin(credentials.adminEmail, credentials.adminPassword);
    cy.visit('/');
    cy.get('#admin-menu').should('not.exist');
    cy.get('#entity-menu').should('not.exist')
    cy.get('a').filter('[href="/user"]').should('not.exist');
    cy.get('a').filter('[href="/assertion"]').should('not.exist');
    cy.get('a').filter('[href="/member"]').should('not.exist');
  });
});
