/// <reference types="cypress" />
import data from '../../fixtures/test-data.json';
import credentials from '../../fixtures/credentials.json';

const { email, name } = data.homepageTestMembers.consortiumMember;

describe('Test editing member details', () => {
  it('Editing Almonds Forest member details', function () {
    const date = Date.now();
    cy.programmaticSignin(email, credentials.password);
    cy.visit('/');
    cy.get('app-member-info-landing')
      .contains(date)
      .should('not.exist');
    cy.visit('/edit');
    cy.get('.text-danger').should('not.exist');
    // wait for data to load
    cy.intercept(`/services/memberservice/api/members/${data.homepageTestMembers.consortiumMember.salesforceId}/member-contacts`).as('details');
    cy.wait('@details');
    cy.get('[name="orgName"]').clear().blur();
    cy.get('small').contains('Organization name cannot be empty');
    cy.get('[name="orgName"]').type(name);
    cy.get('small').contains('Organization cannot be empty').should('not.exist');
    cy.get('[name="publicName"]').clear().blur();
    cy.get('small').contains('Public organization name cannot be empty');
    cy.get('[name="publicName"]').type(name + ' ' + date);
    cy.get('small').contains('Public organization name cannot be empty').should('not.exist');
    cy.get('[name="country"]').invoke('attr', 'readonly').should('exist');
    cy.get('[name="state"]').should('not.exist')
    cy.get('[name="trademarkLicense"][value="Yes"]').click();
    cy.get('[name="street"]').clear().type('Street ' + date);
    cy.get('[name="city"]').clear().type('City ' + date);
    cy.get('[name="postcode"]').clear().type(date);

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
    cy.get('app-member-info-landing').contains('Street ' + date + ", City " + date + ", " + date);
    cy.get('app-member-info-landing').contains(`YES - ORCID can use trademarked assets`);
    cy.visit('/edit');
    // wait for data to load
    cy.intercept(`/services/memberservice/api/members/${data.homepageTestMembers.consortiumMember.salesforceId}/member-contacts`).as('details');
    cy.wait('@details');
    cy.get('[name="trademarkLicense"][value="No"]').click();
    cy.get('[name="website"]').clear()
    cy.get('[type="submit"]').click();
    cy.get('app-member-info-landing').contains(`NO - ORCID cannot use this organization's trademarked name and logos`);
  });
});