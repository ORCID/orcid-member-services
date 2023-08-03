/// <reference types="cypress" />
import data from '../../fixtures/test-data.json';
import credentials from '../../fixtures/credentials.json';

const { email, name } = data.homepageTestMembers.consortiumMember;

describe('Test editing member details', () => {
  it('Editing Almonds forest member dtails', function () {
    const date = Date.now();
    cy.programmaticSignin(email, credentials.password);
    cy.visit('/');
    cy.get('app-member-info-landing')
      .contains(date)
      .should('not.exist');
    cy.visit('/edit');
    cy.get('.text-danger').should('not.exist');
    // cy.intercept('/services/memberservice/api/member-contacts').as('details');
    // cy.wait('@details');
    cy.get('[name="orgName"]').clear().blur();
    cy.get('small').contains('Organization name cannot be empty');
    cy.get('[name="orgName"]').type(name);
    cy.get('small').contains('Organization cannot be empty').should('not.exist');
    cy.get('[name="publicName"]').clear().blur();
    cy.get('small').contains('The organization name cannot be empty');
    cy.get('[name="publicName"]').type(name + ' ' + date);
    cy.get('small').contains('The organization name cannot be empty').should('not.exist');
    cy.get('[name="country"]').invoke('attr', 'readonly').should('exist');
    cy.get('[name="state"]').should('not.exist')

    cy.get('[name="street"]').type('Street ' + date);
    cy.get('[name="city"]').type('City ' + date);
    cy.get('[name="postcode"]').type('Postcode ' + date);

    cy.get('.ql-editor')
      .clear()
      .type('Description: ' + date);

    cy.get('[name="website"]')
      .clear()
      .type(data.invalidWebsite).blur();
    cy.get('.text-danger').should('have.length', 3);
    cy.get('[name="website"]')
      .clear()
      .type('https://' + date + '.org').blur();
    cy.get('.text-danger').should('not.exist');

    cy.get('[name="email"]')
      .clear()
      .type(data.invalidEmail).blur();
    cy.get('.text-danger').should('have.length', 3);
    cy.get('[name="email"]')
      .clear()
      .type(date + '@orcid.org').blur();
    cy.get('.text-danger').should('not.exist');
    cy.get('[type="submit"]').click();
    cy.get('app-member-info-landing', { timeout: 20000 }).contains(`${name} ${date}`);
    cy.get('app-member-info-landing').contains(date + '@orcid.org');
    cy.get('app-member-info-landing').contains('https://' + date + '.org');
    cy.get('app-member-info-landing').contains('Description: ' + date);
    cy.get('app-member-info-landing').contains('Street ' + date + ", City " + date + " ");
  });
});
