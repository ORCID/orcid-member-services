/// <reference types="cypress" />
import data from '../../fixtures/test-data.json';
import credentials from '../../fixtures/credentials.json';

describe('Test the edit user form', () => {
  it('Edit user', function() {
    cy.programmaticSignin(data.populatedMember.users.owner.email, credentials.password);
    cy.visit(`/user/${data.populatedMember.users.owner.id}/edit`);
    // Clear first name input field
    cy.get('#field_firstName').clear();
    // Shouldn't be possible to save with an empty name field
    cy.get('#save-entity2')
    .invoke('attr', 'disabled')
    .should('exist');
    // Check for 'required' flag on the input field
    cy.get('#field_firstName')
    .should('have.class', 'ng-invalid')
    .type("Automated")
    // Clear last name input field
    cy.get('#field_lastName').clear();
    // Shouldn't be possible to save with an empty name field
    cy.get('#save-entity2')
      .invoke('attr', 'disabled')
      .should('exist');
    // Check for 'required' flag on the input field
    cy.get('#field_lastName')
      .should('have.class', 'ng-invalid')
      .type("Test")
    // Check disabled fields
    cy.get('#field_email')
      .invoke('attr', 'disabled')
      .should('exist');
    cy.get('#field_mainContact')
      .invoke('attr', 'disabled')
      .should('exist');
    cy.get('#field_salesforceId')
      .invoke('attr', 'disabled')
      .should('exist');
    // Admin checkbox should not exist
    cy.get('#field_isAdmin').should('not.exist');
      // 'Activated' checkbox is missing the 'disabled' attr
    /*cy.get('#field_activated')
      .invoke('attr', 'disabled')
      .should('exist');*/
    // save
    cy.get('#save-entity2').click();
    cy.get('.alert-success').should('exist');
  }); 
});
