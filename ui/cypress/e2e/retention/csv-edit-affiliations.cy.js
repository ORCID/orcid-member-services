/// <reference types="cypress" />
import data from '../../fixtures/test-data.json';
import credentials from '../../fixtures/credentials.json';
import record from '../../fixtures/csv-populated-orcid-record.json';
import helpers from '../../helpers/helpers.js';
import { recurse } from 'cypress-recurse';
const testString = helpers.newUser.testString;

describe('Test updating affiliations via CSV', () => {
  beforeEach(() => {
    cy.programmaticSignin(data.csvPopulatedMember.users.owner.email, credentials.password);
    cy.visit('/assertion');
  });

  it('Edit the contents of the existing CSV file', function() {
    let editSections = ['department-name', 'org-city', 'org-name', 'role-title'];
    let result = '';
    cy.readFile('./cypress/fixtures/editAffiliations.csv').then(csv => {
      console.log(csv);
      let lines = csv.trim().split('\n');
      let headers = lines[0].split(',');
      result = lines[0] + '\n';
      for (var i = 1; i < lines.length; i++) {
        let currentline = lines[i].split(',');
        for (var j = 0; j < headers.length; j++) {
          if (editSections.includes(headers[j])) currentline[j] = `"${testString}"`;
        }
        result += currentline.join(',') + '\n';
      }
      console.log(result);
      cy.writeFile('./cypress/fixtures/editAffiliations.csv', result);
    })

  });

  it('Upload CSV and check inbox for the confirmation email', function() {
    cy.uploadCsv('../fixtures/editAffiliations.csv');
    cy.task('checkInbox', {
      subject: data.outbox.csvUpload,
      to: data.csvPopulatedMember.users.owner.email
    }).then(email => {  
      const body = email[0].body.html;
      expect(body).to.have.string('The CSV upload was successfully processed with the following results:');
      expect(body).to.have.string('<span>0</span>\r\n\t        &nbsp;\r\n\t        <span>affiliations added');
      expect(body).to.have.string('<span>7</span>\r\n\t        &nbsp;\r\n\t        <span>affiliations updated');
      expect(body).to.have.string('<span>0</span>\r\n\t        &nbsp;\r\n\t        <span>affiliations deleted');
      expect(body).to.have.string('<span>0</span>\r\n\t        &nbsp;\r\n\t        <span>duplicate(s) ignored');
    });
  });

  it('Confirm the changes in the registry', function() {
    recurse(
      () =>
        cy.request({
          url: `https://pub.qa.orcid.org/v3.0/${record.id}/activities`,
          headers: { Accept: 'application/json' }
        }),
      res => {
        const distinction = res.body['distinctions']['affiliation-group'][0]['summaries'][0]['distinction-summary'];
        const education = res.body['educations']['affiliation-group'][0]['summaries'][0]['education-summary'];
        const employment = res.body['employments']['affiliation-group'][0]['summaries'][0]['employment-summary'];
        const invitedPosition = res.body['invited-positions']['affiliation-group'][0]['summaries'][0]['invited-position-summary'];
        const membership = res.body['memberships']['affiliation-group'][0]['summaries'][0]['membership-summary'];
        const qualification = res.body['qualifications']['affiliation-group'][0]['summaries'][0]['qualification-summary'];
        const service = res.body['services']['affiliation-group'][0]['summaries'][0]['service-summary'];
        const trimmedString = testString.trim();
        // cy.checkAffiliationChanges will not retry on fail, which is why we make sure all sections got updated before asserting everything
        expect(distinction['department-name']).to.eq(trimmedString);
        expect(education['department-name']).to.eq(trimmedString);
        expect(employment['department-name']).to.eq(trimmedString);
        expect(invitedPosition['department-name']).to.eq(trimmedString);
        expect(membership['department-name']).to.eq(trimmedString);
        expect(qualification['department-name']).to.eq(trimmedString);
        expect(service['department-name']).to.eq(trimmedString);
        cy.checkAffiliationChanges(distinction, trimmedString);
        cy.checkAffiliationChanges(education, trimmedString);
        cy.checkAffiliationChanges(employment, trimmedString);
        cy.checkAffiliationChanges(invitedPosition, trimmedString);
        cy.checkAffiliationChanges(membership, trimmedString);
        cy.checkAffiliationChanges(qualification, trimmedString);
        cy.checkAffiliationChanges(service, trimmedString);
      },
      {
        log: true,
        limit: 20, // max number of iterations
        timeout: 600000, // time limit in ms
        delay: 30000 // delay before next iteration, ms
      }
    ); 
  })
});
