/// <reference types="cypress" />
import data from '../../fixtures/test-data.json';
import credentials from '../../fixtures/credentials.json';
const { salesforceId, clientName, clientId } = data.member;

describe('Test "Add member" functionality', () => {
  it('Add member', function() {
    cy.programmaticSignin(credentials.adminEmail, credentials.adminPassword);
    cy.visit('/member/new');
    // Check required field flags
    cy.get('#field_salesforceId').should('have.class', 'ng-invalid');
    cy.get('#field_clientName').should('have.class', 'ng-invalid');
    // Save button should be disabled
    cy.get('#save-entity')
      .invoke('attr', 'disabled')
      .should('exist');
    // Check salesforce id warning message when field is clear
    cy.get('#field_salesforceId')
      .type(salesforceId)
      .clear();
    cy.get('small').should('exist');
    cy.get('#save-entity')
      .invoke('attr', 'disabled')
      .should('exist');
    // Enter existing salesforce id to generate an error
    cy.get('#field_salesforceId').type(salesforceId);
    cy.get('#field_parentSalesforceId')
      .type(clientName);
    // Enter invalid client name to generate an error
    cy.get('#field_clientName')
      .type(data.invalidString)
      .clear();
    cy.get('small').should('exist');
    cy.get('#save-entity')
      .invoke('attr', 'disabled')
      .should('exist');
    cy.get('#field_clientName').type(data.populatedMember.clientName);
    cy.get('#save-entity').click();
    // Two error messages should appear for existing salesforce id and member name
    cy.get('.validation-errors')
      .children()
      .should('have.length', 2);
    // Enter invalid client id to generate an error
    cy.get('#field_clientId').type(data.invalidString);
    cy.get('small').should('exist');
    // Check for flag on client id input field
    cy.get('#field_clientId').should('have.class', 'ng-invalid');
    cy.get('#save-entity')
      .invoke('attr', 'disabled')
      .should('exist');
    // Check the enable assertions checkbox
    cy.get('#field_assertionServiceEnabled').check();
    cy.get('#field_clientId')
      .clear()
      .type(clientId);
    cy.get('#field_clientName')
      .clear()
      .type(clientName);
    // Checkbox should be unchecked after clearing client id field
    cy.get('#field_assertionServiceEnabled')
      .should('not.be.checked')
      .check();
    // Parent salesforce id for consortium lead members must match salesforce id or be blank
    cy.get('#field_isConsortiumLead')
      .should('not.be.checked')
      .check();
    cy.get('#save-entity')
      .invoke('attr', 'disabled')
      .should('exist');
    cy.get('#field_parentSalesforceId')
      .clear()
      .type(salesforceId);
    // Save button should be clickable
    cy.get('#save-entity')
      .invoke('attr', 'disabled')
      .should('not.exist');
  });
});
