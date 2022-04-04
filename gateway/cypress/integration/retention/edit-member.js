/// <reference types="cypress" />
import data from '../../fixtures/test-data.json';
import admin from '../../fixtures/admin-data.json';

describe('Manage members menu', () => {
  it('Test edit member form', function() {
    cy.programmaticSignin(admin.email, admin.password)
    cy.visit(`/ms-member/${data.member.id}/edit`)
    cy.get('#field_salesforceId').clear()
    cy.get('#save-entity').invoke('attr', 'disabled').should('exist')
    cy.get('small').filter('[jhitranslate="entity.validation.required.string"]').should('exist')
    cy.get('#field_salesforceId').type("321")
    cy.get('#field_clientName').clear()
    cy.get('#save-entity').invoke('attr', 'disabled').should('exist')
    cy.get('small').filter('[jhitranslate="entity.validation.required.string"]').should('exist')
    cy.get('#field_clientName').type('  !@#$%^&*()-=   проверка   test')
    cy.get('#save-entity').click()
    cy.get('.validation-errors').children().should('have.length', 1)
    cy.get('#field_clientName').type(" 2  ")
    cy.get('#field_clientId').type("A")
    cy.get('small').should('exist')
    cy.get('#save-entity').invoke('attr', 'disabled').should('exist')
    cy.get('#field_clientId').clear()    
    cy.get('#field_clientId').type('APP-9GN1GYLRUONIPZRE')
    cy.get('#field_assertionServiceEnabled').should('not.be.checked').check()
    cy.get('#save-entity').click()
  });
});
