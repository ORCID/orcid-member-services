/// <reference types="cypress" />
import data from '../../fixtures/test-data.json';
import credentials from '../../fixtures/credentials.json';

describe('Add new user', () => {
  it('Reset organization owner', () => {
    cy.programmaticSignin(credentials.adminEmail, credentials.adminPassword)
    cy.visit(`/ms-user/${data.member.users.owner.id}/edit`)
    cy.get("#field_mainContact").click()
    cy.get('#save-entity').click()
    cy.get('.alert-success').should('exist');
  })

  it('Remove all affiliations from test group', function () {
    cy.programmaticSignin(data.member.users.owner.email, credentials.password)
    cy.visit('/assertion')
    cy.get('.btn-group').each($e => {
      cy.wrap($e).children().last().click()
      cy.get('#jhi-confirm-delete-assertion').click()
    })
    cy.visit('/ms-user')
    cy.get('.btn-group').each($e => {
      cy.wrap($e).children().last().invoke('attr', 'disabled').then((disabled) => {
        disabled ? cy.log("Skipping user, button is disabled") : cy.removeAffiliation($e)
      })
    })
    cy.programmaticSignout()    
  })

  it('Remove all affiliations from test group', function () {
    cy.programmaticSignin(data.member.users.owner.email, credentials.password)
    cy.visit('/ms-user')
    cy.get('.btn-group').each($e => {
      cy.wrap($e).children().last().invoke('attr', 'disabled').then((disabled) => {
        disabled ? cy.log("Skipping user, button is disabled") : cy.removeAffiliation($e)
      })
    })
    cy.programmaticSignout()    
  })
})
