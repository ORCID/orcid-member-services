/// <reference types="cypress" />
import data from '../../fixtures/test-data.json';
import credentials from '../../fixtures/credentials.json';

describe('Test sign in form', () => {
  it('Upload CSV', function() {
    cy.programmaticSignin(data.csvMember.users.owner.email, credentials.password);
    cy.visit('/assertion')
    cy.uploadCsv('../fixtures/affiliations.csv');
    let date = new Date()  
    cy.checkInbox(data.outbox.csvUpload, data.csvMember.users.owner.email, date).then(email => {
      const body = email[0].body.html
      expect(body).to.have.string('The CSV upload was successfully processed with the following results:')
      expect(body).to.have.string("<span>7</span>\r\n\t        &nbsp;\r\n\t        <span>affiliations added");
      expect(body).to.have.string("<span>0</span>\r\n\t        &nbsp;\r\n\t        <span>affiliations updated");
      expect(body).to.have.string("<span>0</span>\r\n\t        &nbsp;\r\n\t        <span>affiliations deleted");
      expect(body).to.have.string("<span>2</span>\r\n\t        &nbsp;\r\n\t        <span>duplicate(s) ignored");
    }); 
    cy.readFile('cypress/fixtures/affiliations.csv').then(data => {
      cy.readCsv(data).then(data => { 
        cy.log(data)
      });
    }); 
    cy.visit('/assertion')
    cy.get('#jh-generate-links').click()
    cy.task('checkInbox', {
      options: {
        from: data.outbox.email,
        to: data.csvMember.users.owner.email,
        subject: data.outbox.csvDownload, 
        include_attachments: true, 
        after: Date.now()
      }
    }).then((csv) => {
      const csvContents = Buffer.from(csv[0].attachments[0].data, 'base64').toString('ascii').trim()
      const lines = csvContents.split('\n');
      console.log(lines)
      for (var i=1; i<lines.length; i++) {
        lines[i] = ",,,,,,,,,,,,,,," + lines[i].slice(lines[i].lastIndexOf(','))
      }
      const data = lines.join('\n')
      cy.writeFile('./cypress/fixtures/downloadedAffiliations.csv', data)  
    })  
    
    cy.uploadCsv('../fixtures/downloadedAffiliations.csv');
    date = new Date()  
    cy.checkInbox(data.outbox.csvUpload, data.csvMember.users.owner.email, date).then(email => {
      const body = email[0].body.html
      expect(body).to.have.string('The CSV upload was successfully processed with the following results:')
      expect(body).to.have.string("<span>0</span>\r\n\t        &nbsp;\r\n\t        <span>affiliations added");
      expect(body).to.have.string("<span>0</span>\r\n\t        &nbsp;\r\n\t        <span>affiliations updated");
      expect(body).to.have.string("<span>7</span>\r\n\t        &nbsp;\r\n\t        <span>affiliations deleted");
      expect(body).to.have.string("<span>0</span>\r\n\t        &nbsp;\r\n\t        <span>duplicate(s) ignored");
    }); 
    cy.programmaticSignout()  
  }); 

  /*it('Remove affiliations', function() {
    cy.programmaticSignin(data.csvMember.users.owner.email, credentials.password)
    cy.visit('/assertion')
    cy.get('.btn-group').each($e => {
      cy.wrap($e).children().last().click()
      cy.get('#jhi-confirm-delete-assertion').click()
    })  
    cy.programmaticSignout() 
  }); */
});
