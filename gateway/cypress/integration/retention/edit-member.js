/// <reference types="cypress" />
import data from '../../fixtures/test-data.json';
import credentials from '../../fixtures/credentials.json';
const { salesforceId, clientName, clientId } = data.member;

describe('Manage members menu', () => {
  it('Test edit member form', function() {
    cy.programmaticSignin(credentials.adminEmail, credentials.adminPassword);
    cy.visit(`/member/${data.member.id}/edit`);
    cy.get('#field_isConsortiumLead')
      .should('be.checked')
      .uncheck();
    cy.get('#field_salesforceId')
      .invoke('attr', 'disabled')
      .should('exist');
    cy.get('#field_parentSalesforceId')
      .clear()
      .type(clientName);
    // Shouldn't be able to save without a client name
    cy.get('#field_clientName').clear();
    cy.get('#save-entity')
      .invoke('attr', 'disabled')
      .should('exist');
    cy.get('small')
      .filter('[jhitranslate="entity.validation.required.string"]')
      .should('exist');
    // Enter existing client name and check for relevant error message
    cy.get('#field_clientName').type(data.populatedMember.clientName);
    cy.get('#save-entity').click();
    cy.get('.validation-errors')
      .children()
      .should('have.length', 1);
    cy.get('#field_clientName')
      .clear()
      .type(clientName);
    // Check client id warning message
    cy.get('#field_clientId').type(data.invalidString);
    cy.get('small').should('exist');
    cy.get('#save-entity')
      .invoke('attr', 'disabled')
      .should('exist');
    cy.get('#field_clientId').clear();
    cy.get('#field_clientId').type(clientId);
    // Assertions enabled checkbox should be unchecked after clearing client id field
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
    cy.get('#save-entity')
      .invoke('attr', 'disabled')
      .should('exist');
    cy.get('#field_salesforceId')
      .invoke('val')
      .then(id => {
        cy.get('#field_parentSalesforceId')
          .clear()
          .type(id);
        cy.get('#save-entity').click();
      });
  });
});
