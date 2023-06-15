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

  it('User', function() {
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

    // non org-owners shouldn't be able to access their own userlist
    cy.getUsersBySfId(data.populatedMember.salesforceId, 200);
    cy.getUsersBySfId(data.homepageTestMembers.consortiumLeadAndMember.salesforceId, 400, "Salesforce id doesn't match current user's memeber");
    cy.getAllUsers(403, 'Forbidden');
    cy.getAllMembers(403, 'Forbidden');
    cy.addMember(data.populatedMember.salesforceId, false, false, 403, 'Forbidden');
    cy.updateMember(data.populatedMember.salesforceId, false, false, 403, 'Forbidden');
    cy.validateMember(data.populatedMember.salesforceId, false, false, 403, 'Forbidden');

    // SHOULDN'T PASS FOR OTHER MEMBERS
    //cy.changeNotificationLanguage(data.homepageTestMembers.consortiumLeadAndMember.salesforceId, data.italianLanguageCode, 403, 'Forbidden');

    // CODE ISN'T PUSHED TO QA YET
    /*
    cy.updateContact(data.homepageTestMembers.consortiumLeadAndMember.salesforceId, 403, 'Forbidden');
    cy.updateMemberDetails(data.homepageTestMembers.consortiumLeadAndMember.salesforceId, 403, 'Forbidden');
    cy.getMemberDetails(data.homepageTestMembers.consortiumLeadAndMember.salesforceId, 403, 'Forbidden');
    cy.getMemberContacts(data.homepageTestMembers.consortiumLeadAndMember.salesforceId, 403, 'Forbidden');
    cy.getMemberOrgIds(data.homepageTestMembers.consortiumLeadAndMember.salesforceId, 403, 'Forbidden');
    */

    cy.getMembersList(403, 'Forbidden');

    // SHOULDN'T PASS FOR OTHER MEMBERS AND POSSIBLY FOR OWN MEMBER EITHER
    //cy.getMember(data.homepageTestMembers.consortiumLeadAndMember.salesforceId, 403, 'Forbidden');

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

  it('Org owner', function() {
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

    // SHOULDN'T PASS FOR OTHER MEMBERS
    //cy.changeNotificationLanguage(data.homepageTestMembers.consortiumLeadAndMember.salesforceId, data.italianLanguageCode, 403, 'Forbidden');

    // CODE ISN'T PUSHED TO QA YET
    // SHOULD ADMINS BE ABLE TO DO THIS?
    /*
    cy.updateContact(data.homepageTestMembers.consortiumLeadAndMember.salesforceId, 403, 'Forbidden');
    cy.updateMemberDetails(data.homepageTestMembers.consortiumLeadAndMember.salesforceId, 403, 'Forbidden');
    cy.getMemberDetails(data.homepageTestMembers.consortiumLeadAndMember.salesforceId, 403, 'Forbidden');
    cy.getMemberContacts(data.homepageTestMembers.consortiumLeadAndMember.salesforceId, 403, 'Forbidden');
    cy.getMemberOrgIds(data.homepageTestMembers.consortiumLeadAndMember.salesforceId, 403, 'Forbidden');
    */

    cy.getMembersList(403, 'Forbidden');

    // SHOULDN'T PASS FOR OTHER MEMBERS AND POSSIBLY FOR OWN MEMBER EITHER
    //cy.getMember(data.homepageTestMembers.consortiumLeadAndMember.salesforceId, 403, 'Forbidden');

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

  it('Admin', function() {
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

    // SHOULDN'T PASS FOR OTHER MEMBERS, ANY LANGUAGE CODE CAN BE PROVIDED 
    //cy.changeNotificationLanguage(data.homepageTestMembers.consortiumLeadAndMember.salesforceId, data.italianLanguageCode, 403, 'Forbidden');

    // CODE ISN'T PUSHED TO QA YET
    // SHOULD ADMINS BE ABLE TO DO THIS?
    /*
    cy.updateContact(data.homepageTestMembers.consortiumLeadAndMember.salesforceId, 403, 'Forbidden');
      cy.updateMemberDetails(data.homepageTestMembers.consortiumLeadAndMember.salesforceId, 403, 'Forbidden');
      cy.getMemberDetails(data.homepageTestMembers.consortiumLeadAndMember.salesforceId, 403, 'Forbidden');
      cy.getMemberContacts(data.homepageTestMembers.consortiumLeadAndMember.salesforceId, 403, 'Forbidden');
      cy.getMemberOrgIds(data.homepageTestMembers.consortiumLeadAndMember.salesforceId, 403, 'Forbidden');
      */

    cy.getMembersList(200);

    // SHOULDN'T PASS FOR OTHER MEMBERS AND POSSIBLY FOR OWN MEMBER EITHER
    //cy.getMember(data.homepageTestMembers.consortiumLeadAndMember.salesforceId, 200);

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

  it('Consortium lead', function() {
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

    cy.getUsersBySfId(data.homepageTestMembers.consortiumLeadAndMember.salesforceId, 200);
    cy.getUsersBySfId(data.populatedMember.salesforceId, 400, "Salesforce id doesn't match current user's memeber");
    cy.getAllUsers(403, 'Forbidden');
    cy.getAllMembers(403, 'Forbidden');
    cy.addMember(data.populatedMember.salesforceId, false, false, 403, 'Forbidden');
    cy.updateMember(data.populatedMember.salesforceId, false, false, 403, 'Forbidden');
    cy.validateMember(data.populatedMember.salesforceId, false, false, 403, 'Forbidden');

    // SHOULDN'T PASS FOR OTHER MEMBERS
    //cy.changeNotificationLanguage(data.populatedMember.salesforceId, data.italianLanguageCode, 403, 'Forbidden');

    // CODE ISN'T PUSHED TO QA YET
    /*
    cy.updateContact(data.populatedMember.salesforceId, 403, 'Forbidden');
    cy.updateMemberDetails(data.populatedMember.salesforceId, 403, 'Forbidden');
    cy.getMemberDetails(data.populatedMember.salesforceId, 403, 'Forbidden');
    cy.getMemberContacts(data.populatedMember.salesforceId, 403, 'Forbidden');
    cy.getMemberOrgIds(data.populatedMember.salesforceId, 403, 'Forbidden');
    */

    cy.getMembersList(403, 'Forbidden');

    // SHOULDN'T PASS FOR OTHER MEMBERS AND POSSIBLY FOR OWN MEMBER EITHER
    //cy.getMember(data.homepageTestMembers.consortiumLeadAndMember.salesforceId, 403, 'Forbidden');

    cy.deleteMember(data.homepageTestMembers.consortiumLeadAndMember.salesforceId, 403, 'Forbidden');
    cy.addConsortiumMember(200);
    cy.removeConsortiumMember(200);

    // SHOULDN'T HAVE ACCESS TO ASSERTIONS
    //cy.getAssertions(400, 'Forbidden');
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
