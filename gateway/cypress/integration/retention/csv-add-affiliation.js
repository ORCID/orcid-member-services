/// <reference types="cypress" />
import data from '../../fixtures/test-data.json';
import credentials from '../../fixtures/credentials.json';
import record from '../../fixtures/orcid-record.json';
import { recurse } from 'cypress-recurse';

describe('Test adding affiliations via CSV', () => {
  beforeEach(() => {
    cy.programmaticSignin(data.csvMember.users.owner.email, credentials.password);
    cy.visit('/assertion');
  })
  
  it('Upload CSV and check inbox for the confirmation email', function() {
    cy.uploadCsv('../fixtures/affiliations.csv');
    cy.task('checkInbox', {
      subject: data.outbox.csvUpload,
      to: data.csvMember.users.owner.email,
    }).then(email => {
      const body = email[0].body.html;
      expect(body).to.have.string('The CSV upload was successfully processed with the following results:');
      expect(body).to.have.string('<span>7</span>\r\n\t        &nbsp;\r\n\t        <span>affiliations added');
      expect(body).to.have.string('<span>0</span>\r\n\t        &nbsp;\r\n\t        <span>affiliations updated');
      expect(body).to.have.string('<span>0</span>\r\n\t        &nbsp;\r\n\t        <span>affiliations deleted');
      expect(body).to.have.string('<span>2</span>\r\n\t        &nbsp;\r\n\t        <span>duplicate(s) ignored');
    });
  });

  it('Grant permission and confirm that the affiliations were added to the UI and the registry', function() {
    cy.get('tbody').within(() => {
      cy.get('tr').each(($e) => {
        cy.wrap($e).children().eq(0).contains(record.email);
        cy.wrap($e).children().eq(1).should('not.contain', record.id);
        cy.wrap($e).children().eq(4).contains('Pending');
      })
    })
    cy.fetchLinkAndGrantPermission();

    recurse(
      () =>
        cy.request({
          url: `https://pub.qa.orcid.org/v3.0/${record.id}/activities`,
          headers: { Accept: 'application/json' }
        }),
      res => {
        expect(res.body['distinctions']['affiliation-group']).to.have.length(1);
        expect(res.body['educations']['affiliation-group']).to.have.length(1);
        expect(res.body['employments']['affiliation-group']).to.have.length(1);
        expect(res.body['invited-positions']['affiliation-group']).to.have.length(1);
        expect(res.body['memberships']['affiliation-group']).to.have.length(1);
        expect(res.body['qualifications']['affiliation-group']).to.have.length(1);
        expect(res.body['services']['affiliation-group']).to.have.length(1);
      },
      {
        log: true,
        limit: 20, // max number of iterations
        timeout: 600000, // time limit in ms
        delay: 30000 // delay before next iteration, ms
      }
    );   
  }); 

  it ('Check that the statuses of the affiliations have changed to "In ORCID"', function() {
    cy.get('tbody').within(() => {
      cy.get('tr').each(($e) => {
        cy.wrap($e).children().eq(1).contains(record.id);
        cy.wrap($e).children().eq(4).contains('In ORCID');
      })
    })
  })

  it('Download the CSV and edit the contents to have the affiliations removed', function() {
    cy.get('#jh-generate-csv').click();
    cy.task('checkInbox', {
      to: data.csvMember.users.owner.email,
      subject: data.outbox.csvDownload,
      include_attachments: true,
    }).then(csv => {
      const csvContents = Buffer.from(csv[0].attachments[0].data, 'base64')
        .toString('ascii')
        .trim();
      const lines = csvContents.split('\n');
      console.log(lines);
      for (var i = 1; i < lines.length; i++) {
        lines[i] = ',,,,,,,,,,,,,,,' + lines[i].slice(lines[i].lastIndexOf(','));
      }
      const data = lines.join('\n');
      cy.writeFile('./cypress/fixtures/downloadedAffiliations.csv', data);
    });  
  });

  it('Upload second CSV and check inbox for the confirmation email', function() {
    cy.uploadCsv('../fixtures/downloadedAffiliations.csv'); 
    cy.task('checkInbox', {
      subject: data.outbox.csvUpload,
      to: data.csvMember.users.owner.email,
    }).then(email => {
      const body = email[0].body.html;
      expect(body).to.have.string('The CSV upload was successfully processed with the following results:');
      expect(body).to.have.string('<span>0</span>\r\n\t        &nbsp;\r\n\t        <span>affiliations added');
      expect(body).to.have.string('<span>0</span>\r\n\t        &nbsp;\r\n\t        <span>affiliations updated');
      expect(body).to.have.string('<span>7</span>\r\n\t        &nbsp;\r\n\t        <span>affiliations deleted');
      expect(body).to.have.string('<span>0</span>\r\n\t        &nbsp;\r\n\t        <span>duplicate(s) ignored');
    });
  });

  it ('Confirm that the affiliations have been removed from the UI and the registry', function() {
    recurse(
      () =>
        cy.request({
          url: `https://pub.qa.orcid.org/v3.0/${record.id}/activities`,
          headers: { Accept: 'application/json' }
        }),
      res => {
        expect(res.body['distinctions']['affiliation-group']).to.have.length(0);
        expect(res.body['educations']['affiliation-group']).to.have.length(0);
        expect(res.body['employments']['affiliation-group']).to.have.length(0);
        expect(res.body['invited-positions']['affiliation-group']).to.have.length(0);
        expect(res.body['memberships']['affiliation-group']).to.have.length(0);
        expect(res.body['qualifications']['affiliation-group']).to.have.length(0);
        expect(res.body['services']['affiliation-group']).to.have.length(0);
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
