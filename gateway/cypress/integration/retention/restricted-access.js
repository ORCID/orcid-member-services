/// <reference types="cypress" />
import data from '../../fixtures/test-data.json';
import credentials from '../../fixtures/credentials.json';

describe('Test restricted access', () => {
  afterEach(() => {
    cy.programmaticSignout();
  })

  it('Regular users should not be able to access the Manage Members menu', function() {
    cy.programmaticSignin(data.populatedMember.users.user.email, credentials.password);
    cy.visit('/member');
    cy.get('h1').filter('[jhitranslate="error.title.string"]').contains('Your request cannot be processed')
    cy.get('div').filter('[jhitranslate="error.http.403.string"]').invoke('attr', 'hidden').should('not.exist')
  });

  it('Regular users should not be able to access the Manage Users menu', function() {
    cy.programmaticSignin(data.populatedMember.users.user.email, credentials.password);
    cy.visit('/user');
    cy.get('h1').filter('[jhitranslate="error.title.string"]').contains('Your request cannot be processed')
    cy.get('div').filter('[jhitranslate="error.http.403.string"]').invoke('attr', 'hidden').should('not.exist')
  });

  it('Org owners should not be able to access the Manage Members menu', function() {
    cy.programmaticSignin(data.populatedMember.users.owner.email, credentials.password);
    cy.visit('/member');
    cy.get('h1').filter('[jhitranslate="error.title.string"]').contains('Your request cannot be processed')
    cy.get('div').filter('[jhitranslate="error.http.403.string"]').invoke('attr', 'hidden').should('not.exist')
  });

  it('Cannot access the assertion menu if assertions are not enabled', function() {
    cy.programmaticSignin(data.homepageTestMembers.consortiumLeadAndMemberEmail, credentials.password);
    cy.visit('/assertion');
    cy.get('h1').filter('[jhitranslate="error.title.string"]').contains('Your request cannot be processed')
    cy.get('div').filter('[jhitranslate="error.http.403.string"]').invoke('attr', 'hidden').should('not.exist')
  });

  it('Anonymous users cannot access anything', function() {
    cy.visit('/assertion');
    cy.get('h1').filter('[jhitranslate="error.title.string"]').contains('Your request cannot be processed')
    cy.get('div').filter('[jhitranslate="error.http.403.string"]').invoke('attr', 'hidden').should('not.exist')
    cy.visit('/user');
    cy.get('h1').filter('[jhitranslate="error.title.string"]').contains('Your request cannot be processed')
    cy.get('div').filter('[jhitranslate="error.http.403.string"]').invoke('attr', 'hidden').should('not.exist')
    cy.visit('/member');
    cy.get('h1').filter('[jhitranslate="error.title.string"]').contains('Your request cannot be processed')
    cy.get('div').filter('[jhitranslate="error.http.403.string"]').invoke('attr', 'hidden').should('not.exist')
  });
});
