/// <reference types="cypress" />
import data from '../../fixtures/test-data.json';
import record from '../../fixtures/populated-orcid-record.json';
import credentials from '../../fixtures/credentials.json';
import helpers from '../../helpers/helpers.js';
import { recurse } from 'cypress-recurse';
const testString = helpers.newUser.testString;

describe('Edit an affiliation', () => {
  it('Edit affiliation in the member portal', function() {
    cy.programmaticSignin(data.populatedMember.users.owner.email, credentials.password);
    cy.visit(`/assertion/${record.affiliation.id}/edit`);

    cy.get('#field_orgName').clear().type(testString);
    cy.get('#field_orgCity').clear().type(testString);
    cy.get('#field_departmentName').clear().type(testString);
    cy.get('#field_roleTitle').clear().type(testString);

    cy.get('#save-entity')
      .click();
    cy.get('.alert-success').should('exist');
    
  });

  it('Confirm the affiliation has been updated in the registry', () => {
    recurse(
      () =>
        cy.request({
          url: `https://pub.qa.orcid.org/v3.0/${record.id}/educations`,
          headers: { Accept: 'application/json' }
        }),
      res => {
        const education = res.body['affiliation-group'][0]['summaries'][0]['education-summary'];
        expect(res.body['affiliation-group']).to.have.length(1);
        expect(education['department-name']).to.eq(testString);
        expect(education['role-title']).to.eq(testString);
        expect(education['organization']['address']['city']).to.eq(testString);
        expect(education['organization']['name']).to.eq(testString);
        expect(education['department-name']).to.eq(testString);
      },
      {
        log: true,
        limit: 10, // max number of iterations
        timeout: 180000, // time limit in ms
        delay: 30000 // delay before next iteration, ms
      }
    );
  });
});
