/// <reference types="cypress" />
import data from '../../fixtures/test-data.json';
import record from '../../fixtures/orcid-record.json';
import credentials from '../../fixtures/credentials.json';

const { country, type } = record.affiliation;
const { ror } = record.affiliation.org;

describe('Test notifications', () => {
  it('Add an affiliation with an email missing from the registry', function () {
    cy.programmaticSignin(data.notificationsMember.users.owner.email, credentials.password);
    cy.visit('/assertion/new');
    cy.get('#field_email').type(record.nonregisteredEmail);
    cy.get('#field_affiliationSection').select(type);
    cy.get('#field_orgName').type(data.testString);
    cy.get('#field_orgCity').type(data.testString);
    cy.get('#field_orgCountry').select(country);
    cy.get('#field_disambiguationSource').select(ror.name);
    cy.get('#field_disambiguatedOrgId').type(ror.id);
    cy.get('#save-entity').click();
    cy.get('.alert-success').should('exist');
    cy.programmaticSignout();
  });

  it('Add an affiliation with an email present in the registry and generate notifications', function () {
    cy.programmaticSignin(data.notificationsMember.users.owner.email, credentials.password);
    cy.visit('/assertion/new');
    cy.get('#field_email').type(record.email);
    cy.get('#field_affiliationSection').select(type);
    cy.get('#field_orgName').type(data.testString + ' 2');
    cy.get('#field_orgCity').type(data.testString + ' 2');
    cy.get('#field_orgCountry').select(country);
    cy.get('#field_disambiguationSource').select(ror.name);
    cy.get('#field_disambiguatedOrgId').type(ror.id);
    cy.get('#save-entity').click();
    cy.get('.alert-success').should('exist');
    cy.visit('/assertion');
    cy.get('#jh-send-notifications').click();
    cy.get('#langKey').should('have.value', data.italianLanguageCode);
    cy.get('#jhi-confirm-csv-upload').click();
    cy.get('.alert-success').should('exist');
    cy.get('tbody').children().should('have.length', 2);
    cy.get('tbody').children().eq(0).children().eq(4).contains('Notification requested');
    cy.get('tbody').children().eq(1).children().eq(4).contains('Notification requested');
    cy.programmaticSignout();
  });

  it('Check inbox for succesful notfications confirmation', function () {
    cy.task('checkInbox', {
      subject: data.outbox.notificationConfirmation,
      to: data.notificationsMember.users.owner.email,
    }).then(email => {
      const body = email[0].body.html;
      expect(body).to.have.string('Thank you for choosing to use the permission notification process.');
      expect(body).to.have.string('<span>1</span>\r\n        &nbsp;\r\n        <span>ORCID inbox notifications</span>');
      expect(body).to.have.string('<span>1</span>\r\n        &nbsp;\r\n        <span>emails</span>');
    });
  });

  it('Check inbox for translated notification for non-registered email', function () {
    cy.task('checkInbox', {
      subject: data.outbox.notificationNonRegisteredUserItalian,
      to: record.nonregisteredEmail,
    }).then(email => {
      const body = email[0].body.html;
      cy.log(body);
      expect(body).to.have.string(
        'Consentendo a NOTIFICATIONS TEST di aggiungere le informazioni di ricerca al tuo record ORCID, puoi dedicare più tempo alla ricerca e meno alla gestione! Prima però avremo bisogno del tuo permesso, quindi clicca sul link di seguito per iniziare. Ti reindirizzeremo su ORCID; dopo aver eseguito l’accesso, clicca su “autorizza accesso” per consentire a NOTIFICATIONS TEST di aggiungere informazioni al tuo record'
      );
      expect(body).to.have.string(
        'Dopo aver concesso il permesso, al tuo record ORCID verrà aggiunta una voce di affiliazione. Tieni presente che potrebbero volerci fino a 5 minuti per l’aggiornamento del tuo record ORCID.'
      );
    });
  });

  it('Check inbox for translated notification for the registered email', function () {
    cy.task('checkInbox', {
      subject: data.outbox.notificationRegisteredUser,
      to: record.email,
      from: data.outbox.updateEmail,
    }).then(email => {
      const body = email[0].body.html;
      expect(body).to.have.string(
        'Le affiliazioni possono essere aggiunte al tuo archivio dalle organizzazioni a te associate. Questo ti fa risparmiare tempo e aumenta il livello di fiducia in archivio.'
      );
      expect(body).to.have.string(
        'Grant permission                                    </span>\r\n                                </button>'
      );
      expect(body).to.have.string('href="https://qa.orcid.org/inbox/encrypted/');
      expect(body).to.have.string(
        'ORCID Member Portal - QA\r\n                             has asked for permission to make changes to your ORCID record'
      );
    });
  });

  it('Delete added affiliations', function () {
    cy.programmaticSignin(data.notificationsMember.users.owner.email, credentials.password);
    cy.visit('/assertion');
    cy.get('.btn-group').each($e => {
      cy.wrap($e).children().last().click();
      cy.get('#jhi-confirm-delete-assertion').click();
    });
    cy.programmaticSignout();
  });
});
