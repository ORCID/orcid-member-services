/// <reference types="cypress" />
import data from '../../fixtures/test-data.json';
import credentials from '../../fixtures/credentials.json';

describe('Test pagination', () => {
  beforeEach(() => {
    cy.programmaticSignin(data.paginationMember.users.owner.email, credentials.password);
  })

  afterEach(() => {
    cy.programmaticSignout();
  })

  it('Test the "Manage users" page', function() {
    cy.visit('/user');
    /*
    cy.get('.container-fluid').within(() => {
      cy.get('p')
      .contains('Showing 1 - 20 of 22 items')
    })*/
    cy.get('tbody').children().should('have.length', 20);
    cy.get('.pagination').contains("1");
    cy.get('.pagination').contains("3").should('not.exist');
    cy.get('.pagination').contains("2").click();
    cy.get('tbody').children().should('have.length', 2);
    cy.get('tbody').within(() => {
      cy.get('td').contains(data.paginationMember.assertionEmail1).should('not.exist');
      cy.get('td').contains(data.paginationMember.assertionEmail2);
      cy.get('td').contains(data.paginationMember.assertionEmail3);
    })
  });

  it('Test the "Affiliations" page', function() {
    // Pip to-do
  });
});
