/// <reference types="cypress" />
import data from '../../fixtures/test-data.json';
import admin from '../../fixtures/admin-data.json';

describe('Add new user', () => {
  it('Reset organization owner', () => {
    cy.programmaticSignin(admin.email, admin.password)
    cy.visit(`/ms-user/${data.member.users.owner.id}/edit`)
    cy.get("#field_mainContact").click()
    cy.get('#save-entity').click()
  })

  it('Remove all affiliations from test group', function () {
    cy.programmaticSignin(data.member.users.owner.email, data.password)
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
    cy.programmaticSignin(data.member.users.owner.email, data.password)
    cy.visit('/ms-user')
    cy.get('.btn-group').each($e => {
      cy.wrap($e).children().last().invoke('attr', 'disabled').then((disabled) => {
        disabled ? cy.log("Skipping user, button is disabled") : cy.removeAffiliation($e)
      })
    })
    cy.programmaticSignout()    
  })
})
