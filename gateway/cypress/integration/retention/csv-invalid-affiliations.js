/// <reference types="cypress" />
import data from '../../fixtures/test-data.json';
import credentials from '../../fixtures/credentials.json';

describe('Test invalid CSV upload', () => {
  it('Upload CSV and check inbox for the error message', function () {
    cy.programmaticSignin(data.csvMember.users.owner.email, credentials.password);
    cy.visit('/assertion');
    cy.uploadCsv('../fixtures/invalidAffiliations.csv');
    cy.task('checkInbox', {
      subject: data.outbox.csvUpload,
      to: data.csvMember.users.owner.email,
    }).then(email => {
      const body = email[0].body.html;
      expect(body).to.have.string('There was a problem with your CSV upload. Pleases fix the errors below and try again.');
    });
  });
});
