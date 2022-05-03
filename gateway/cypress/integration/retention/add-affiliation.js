/// <reference types="cypress" />
import data from '../../fixtures/test-data.json';
import record from '../../fixtures/orcid-record.json';
import credentials from '../../fixtures/credentials.json';
import { recurse } from 'cypress-recurse';

const months = {
  January: '01',
  February: '02',
  March: '03',
  April: '04',
  May: '05',
  June: '06',
  July: '07',
  August: '08',
  September: '09',
  October: '10',
  November: '11',
  December: '12'
};
const { country, countryCode, url, invalidUrl, startDate, endDate, type } = record.affiliation;
const { ringgold, grid, ror } = record.affiliation.org;
describe('Add and remove affiliation', () => {
 /* beforeEach(() => {
    cy.programmaticSignin(data.member.users.owner.email, credentials.password);
  });

  afterEach(() => {
    cy.programmaticSignout();
  });*/

  it('Add affiliation', function() {
    cy.programmaticSignin(data.member.users.owner.email, credentials.password);
    cy.visit('/assertion/new');

    cy.get('#field_email').type(record.invalidEmail);
    cy.get('#field_affiliationSection').select(type);
    cy.get('#field_orgName').type(data.testString);
    cy.get('#field_orgCity').type(data.testString);
    cy.get('#field_orgCountry').select(country);
    cy.checkOrgId(ringgold.name, ringgold.invalidId, ringgold.id);
    cy.checkOrgId(grid.name, grid.invalidId, grid.id);
    cy.checkOrgId(ror.name, ror.invalidId, ror.id);
    cy.get('#field_departmentName').type(data.testString);
    cy.get('#field_roleTitle').type(data.testString);
    cy.get('#field_url').type(invalidUrl);
    cy.get('#field_startYear').select(startDate.year);
    cy.get('#field_startMonth').select(startDate.month);
    cy.get('#field_startDay').select(startDate.invalidDay);
    cy.get('#field_endYear').select(endDate.year);
    cy.get('#field_endMonth').select(endDate.month);
    cy.get('#field_endDay').select(endDate.day);
    cy.get('small')
      .filter('[jhitranslate="entity.validation.endDate.string"]')
      .should('exist');
    cy.get('#save-entity')
      .invoke('attr', 'disabled')
      .should('exist');
    cy.get('#field_startDay').select(startDate.day);
    cy.get('small').should('not.exist');
    cy.get('#save-entity').click();
    cy.get('.alerts')
      .children()
      .should('have.length', 1);
    cy.get('#field_email')
      .clear()
      .type(record.email);
    cy.get('#save-entity').click();
    cy.get('.alerts')
      .children()
      .should('have.length', 2);
    cy.get('#field_url')
      .clear()
      .type(url)
      .get('#save-entity')
      .click();
    cy.get('.alert-success').should('exist');
    cy.programmaticSignout();
  });

  it('Grant permission and check ORCID record for added affiliation', () => {
    cy.programmaticSignin(data.member.users.owner.email, credentials.password);
    // Get permission link
    cy.visit('/assertion');
    cy.get('tbody').children().first().children().eq(1).contains(record.email);
    cy.get('tbody').children().first().children().eq(2).should('not.contain', record.id);
    cy.get('tbody').children().first().children().eq(3).contains(record.affiliation.type);
    cy.get('tbody').children().first().children().eq(4).contains('Pending');
    
    cy.fetchLinkAndGrantPermission();

    recurse(
      () =>
        cy.request({
          url: `https://pub.qa.orcid.org/v3.0/${record.id}/services`,
          headers: { Accept: 'application/json' }
        }),
      res => {
        const service = res.body['affiliation-group'][0]['summaries'][0]['service-summary'];
        expect(res.body['affiliation-group']).to.have.length(1);
        expect(service['department-name']).to.eq(data.testString);
        expect(service['role-title']).to.eq(data.testString);
        expect(service['organization']['address']['city']).to.eq(data.testString);
        expect(service['organization']['address']['country']).to.eq(countryCode);
        expect(service['organization']['name']).to.eq(data.testString);
        expect(service['department-name']).to.eq(data.testString);
        expect(service['url']['value']).to.eq(url);
        expect(service['start-date']['year']['value']).to.eq(startDate.year);
        expect(service['start-date']['month']['value']).to.eq(months[startDate.month]);
        expect(service['start-date']['day']['value']).to.eq(startDate.day);
        expect(service['end-date']['year']['value']).to.eq(endDate.year);
        expect(service['end-date']['month']['value']).to.eq(months[endDate.month]);
        expect(service['end-date']['day']['value']).to.eq(endDate.day);
        expect(service['organization']['disambiguated-organization']['disambiguated-organization-identifier']).to.eq(ror.id);
        expect(service['organization']['disambiguated-organization']['disambiguation-source']).to.eq(ror.name);
        expect(service['department-name']).to.eq(data.testString);
      },
      {
        log: true,
        limit: 20, // max number of iterations
        timeout: 600000, // time limit in ms
        delay: 30000 // delay before next iteration, ms
      }
    );    
  });

  it('Confirm UI changes on the assertion page', () => {
    cy.programmaticSignin(data.member.users.owner.email, credentials.password);
    cy.visit('/assertion');
    cy.get('tbody').children().first().children().eq(2).contains(record.id);
    cy.get('tbody').children().first().children().eq(4).contains('In ORCID');
    cy.programmaticSignout();
  })

  it('Delete affiliation', () => {
    cy.programmaticSignin(data.member.users.owner.email, credentials.password);
    cy.visit('/assertion');
    cy.get('.btn-group').each($e => {
      cy.wrap($e)
        .children()
        .last()
        .click();
      cy.get('#jhi-confirm-delete-assertion').click();
    });
    recurse(
      () =>
        cy.request({
          url: `https://pub.qa.orcid.org/v3.0/${record.id}/services`,
          headers: { Accept: 'application/json' }
        }),
      res => {
        console.log(res);
        expect(res.body['affiliation-group']).to.have.length(0);
      },
      {
        log: true,
        limit: 20, // max number of iterations
        timeout: 600000, // time limit in ms
        delay: 30000 // delay before next iteration, ms
      }
    );
    cy.programmaticSignout();
  });
});
