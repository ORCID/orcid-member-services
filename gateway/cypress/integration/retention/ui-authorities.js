/// <reference types="cypress" />
import data from '../../fixtures/test-data.json';
import credentials from '../../fixtures/credentials.json';

describe('Test authorities', () => {
  afterEach(() => {
    cy.programmaticSignout();
  });

  /* ************************************************************************************************************************
   * ************************************************************************************************************************
   *    REGULAR USER
   * ************************************************************************************************************************
   * ************************************************************************************************************************
   */

  it('User', function () {
    cy.programmaticSignin(data.populatedMember.users.user.email, credentials.password);
    cy.visit('/');
    cy.get('#admin-menu').should('not.exist');
    cy.get('#entity-menu').should('exist');
    cy.get('a')
      .filter('[href="/assertion"]')
      .should('exist');
    cy.get('a')
      .filter('[href="/user"]')
      .should('not.exist');
    cy.get('a')
      .filter('[href="/member"]')
      .should('not.exist');

    cy.getUsersBySfId(data.populatedMember.salesforceId, 401);
    cy.getUsersBySfId(data.homepageTestMembers.consortiumLeadAndMember.salesforceId, 400, "Salesforce id doesn't match current user's memeber");
    cy.getAllUsers(403, 'Forbidden');
    cy.getAllMembers(403, 'Forbidden');
    cy.addMember(data.populatedMember.salesforceId, false, false, 403, 'Forbidden');
    cy.updateMember(data.populatedMember.salesforceId, false, false, 403, 'Forbidden');
    cy.validateMember(data.populatedMember.salesforceId, false, false, 403, 'Forbidden');

    cy.changeNotificationLanguage(data.homepageTestMembers.consortiumLeadAndMember.salesforceId, data.italianLanguageCode, 401, 'Unauthorized');

    cy.updateContact(data.homepageTestMembers.consortiumLeadAndMember.salesforceId, 401, 'Unauthorized');
    cy.updateMemberDetails(data.homepageTestMembers.consortiumLeadAndMember.salesforceId, "The Harvest Ascendancy", 401, 'Unauthorized');
    cy.getMemberDetails(data.homepageTestMembers.consortiumLeadAndMember.salesforceId, 401, 'Unauthorized');
    cy.getMemberContacts(data.homepageTestMembers.consortiumLeadAndMember.salesforceId, 401, 'Unauthorized');
    cy.getMemberOrgIds(data.homepageTestMembers.consortiumLeadAndMember.salesforceId, 401, 'Unauthorized');


    cy.getMembersList(403, 'Forbidden');

    // Awaiting endpoint changes
    cy.getMember(data.homepageTestMembers.consortiumLeadAndMember.salesforceId, 200);

    cy.deleteMember(data.populatedMember.salesforceId, 403, 'Forbidden');

    cy.addConsortiumMember(403, 'Forbidden');
    cy.removeConsortiumMember(403, 'Forbidden');

    cy.getAssertions(200);
  });

  /* ************************************************************************************************************************
   * ************************************************************************************************************************
   *    ORG OWNER
   * ************************************************************************************************************************
   * ************************************************************************************************************************
   */

  it('Org owner', function () {
    cy.programmaticSignin(data.populatedMember.users.owner.email, credentials.password);
    cy.visit('/');
    cy.get('#admin-menu').should('exist');
    cy.get('#entity-menu').should('exist');
    cy.get('a')
      .filter('[href="/user"]')
      .should('exist');
    cy.get('a')
      .filter('[href="/assertion"]')
      .should('exist');
    cy.get('a')
      .filter('[href="/member"]')
      .should('not.exist');

    cy.getUsersBySfId(data.populatedMember.salesforceId, 200);
    cy.getUsersBySfId(data.homepageTestMembers.consortiumLeadAndMember.salesforceId, 400, "Salesforce id doesn't match current user's memeber");
    cy.getAllUsers(403, 'Forbidden');
    cy.getAllMembers(403, 'Forbidden');
    cy.addMember(data.populatedMember.salesforceId, false, false, 403, 'Forbidden');
    cy.updateMember(data.populatedMember.salesforceId, false, false, 403, 'Forbidden');
    cy.validateMember(data.populatedMember.salesforceId, false, false, 403, 'Forbidden');

    cy.changeNotificationLanguage(data.homepageTestMembers.consortiumLeadAndMember.salesforceId, data.italianLanguageCode, 401, 'Unauthorized');

    cy.updateContact(data.homepageTestMembers.consortiumLeadAndMember.salesforceId, 401, 'Unauthorized');
    cy.updateMemberDetails(data.homepageTestMembers.consortiumLeadAndMember.salesforceId, 'The Harvest Ascendancy', 401, 'Unauthorized');
    cy.getMemberDetails(data.homepageTestMembers.consortiumLeadAndMember.salesforceId, 401, 'Unauthorized');
    cy.getMemberContacts(data.homepageTestMembers.consortiumLeadAndMember.salesforceId, 401, 'Unauthorized');
    cy.getMemberOrgIds(data.homepageTestMembers.consortiumLeadAndMember.salesforceId, 401, 'Unauthorized');


    cy.getMembersList(403, 'Forbidden');

    // Awaiting endpoint changes
    cy.getMember(data.homepageTestMembers.consortiumLeadAndMember.salesforceId, 200);

    cy.addConsortiumMember(403, 'Forbidden');
    cy.removeConsortiumMember(403, 'Forbidden');

    cy.deleteMember(data.populatedMember.salesforceId, 403, 'Forbidden');
    cy.getAssertions(200);
  });

  /* ************************************************************************************************************************
   * ************************************************************************************************************************
   *    ADMIN
   * ************************************************************************************************************************
   * ************************************************************************************************************************
   */

  it('Admin', function () {
    cy.programmaticSignin(credentials.adminEmail, credentials.adminPassword);
    cy.visit('/');
    cy.get('#admin-menu').should('exist');
    cy.get('#entity-menu').should('exist');
    cy.get('a')
      .filter('[href="/user"]')
      .should('exist');
    cy.get('a')
      .filter('[href="/assertion"]')
      .should('exist');
    cy.get('a')
      .filter('[href="/member"]')
      .should('exist');

    cy.getUsersBySfId(data.populatedMember.salesforceId, 400, "Salesforce id doesn't match current user's memeber");
    cy.getAllUsers(200);
    cy.getAllMembers(200);
    cy.addMember(data.populatedMember.salesforceId, false, false, 400, 'Member invalid');
    cy.updateMember(data.invalidString, false, false, 500, 'Internal Server Error');
    cy.validateMember(data.populatedMember.salesforceId, false, false, 200);

    cy.changeNotificationLanguage(data.homepageTestMembers.consortiumLeadAndMember.salesforceId, data.italianLanguageCode, 401, 'Unauthorized');

    cy.updateContact(data.homepageTestMembers.consortiumLeadAndMember.salesforceId, 401, 'Unauthorized');
    cy.updateMemberDetails(data.homepageTestMembers.consortiumLeadAndMember.salesforceId, 'The Harvest Ascendancy', 401, 'Unauthorized');
    cy.getMemberDetails(data.homepageTestMembers.consortiumLeadAndMember.salesforceId, 401, 'Unauthorized');
    cy.getMemberContacts(data.homepageTestMembers.consortiumLeadAndMember.salesforceId, 401, 'Unauthorized');
    cy.getMemberOrgIds(data.homepageTestMembers.consortiumLeadAndMember.salesforceId, 401, 'Unauthorized');

    cy.getMembersList(200);

    // Awaiting endpoint changes
    cy.getMember(data.homepageTestMembers.consortiumLeadAndMember.salesforceId, 200);

    cy.addConsortiumMember(403, 'Forbidden');
    cy.removeConsortiumMember(403, 'Forbidden');

    cy.deleteMember(data.invalidString, 400, 'Invalid id');
    cy.getAssertions(200);
  });

  /* ************************************************************************************************************************
   * ************************************************************************************************************************
   *    CONSORTIUM LEAD / AFFILIATION MANAGER DISABLED
   * ************************************************************************************************************************
   * ************************************************************************************************************************
   */

  it('Consortium lead', function () {
    cy.programmaticSignin(data.homepageTestMembers.consortiumLeadAndMember.email, credentials.password);
    cy.visit('/');
    cy.get('#admin-menu').should('exist');
    cy.get('#entity-menu').should('exist');
    cy.get('a')
      .filter('[href="/user"]')
      .should('exist');
    cy.get('a')
      .filter('[href="/assertion"]')
      .should('not.exist');
    cy.get('a')
      .filter('[href="/member"]')
      .should('not.exist');

    cy.getUsersBySfId(data.homepageTestMembers.consortiumLeadAndMember.salesforceId, 401);
    cy.getUsersBySfId(data.populatedMember.salesforceId, 400, "Salesforce id doesn't match current user's memeber");
    cy.getAllUsers(403, 'Forbidden');
    cy.getAllMembers(403, 'Forbidden');
    cy.addMember(data.populatedMember.salesforceId, false, false, 403, 'Forbidden');
    cy.updateMember(data.populatedMember.salesforceId, false, false, 403, 'Forbidden');
    cy.validateMember(data.populatedMember.salesforceId, false, false, 403, 'Forbidden');

    cy.changeNotificationLanguage(data.populatedMember.salesforceId, data.italianLanguageCode, 401, 'Unauthorized');

    cy.updateContact(data.populatedMember.salesforceId, 401, 'Unauthorized');
    cy.updateMemberDetails(data.populatedMember.salesforceId, "Test", 401, 'Unauthorized');
    cy.getMemberDetails(data.populatedMember.salesforceId, 401, 'Unauthorized');
    cy.getMemberContacts(data.populatedMember.salesforceId, 401, 'Unauthorized');
    cy.getMemberOrgIds(data.populatedMember.salesforceId, 401, 'Unauthorized');

    cy.getMembersList(403, 'Forbidden');

    // Awaiting endpoint changes
    cy.getMember(data.homepageTestMembers.consortiumLeadAndMember.salesforceId, 200);

    cy.deleteMember(data.homepageTestMembers.consortiumLeadAndMember.salesforceId, 403, 'Forbidden');
    cy.addConsortiumMember(200);
    cy.removeConsortiumMember(200);

    // Awaiting endpoint changes
    cy.getAssertions(200);
  });

  // TODO: enable once the issue with signed out users not being able to visit routes is fixed
  /* it('Anonymous', function() {
    cy.programmaticSignin(credentials.adminEmail, credentials.adminPassword);
    cy.visit('/');
    cy.get('#admin-menu').should('not.exist');
    cy.get('#entity-menu').should('not.exist')
    cy.get('a').filter('[href="/user"]').should('not.exist');
    cy.get('a').filter('[href="/assertion"]').should('not.exist');
    cy.get('a').filter('[href="/member"]').should('not.exist');
  }); */
});
