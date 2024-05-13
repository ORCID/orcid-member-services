/// <reference types="cypress" />
import data from '../../fixtures/test-data.json';
import credentials from '../../fixtures/credentials.json';

describe('Test homepage', () => {
  it('Direct member', function() {
    cy.programmaticSignin(data.homepageTestMembers.directMemberEmail, credentials.password);
    cy.visit('/');
    cy.get('app-member-info-landing', {timeout: 20000})
    cy.get('app-generic-landing').should('not.exist')
    cy.get('.side-bar').contains("Public details")
    cy.get('.side-bar').contains("Website")
    cy.get('.side-bar').contains("Email")
    cy.get('.side-bar').contains("https://orcid.org")
    cy.get('.side-bar').contains("orcid@orcid.org")
    cy.get('.main-section').contains("Membership: Active")
    cy.get('.main-section').contains('Fly and Mighty')
    cy.get('.main-section').contains('Contacts')
    cy.get('.main-section').contains('Main relationship contact (OFFICIAL)')
    cy.get('.main-section').contains('h.hanger@testingthisemail.com')
  });

  it('Consortium lead', function() {
    cy.programmaticSignin(data.homepageTestMembers.consortiumLeadEmail, credentials.password);
    cy.visit('/');
    cy.get('app-member-info-landing', {timeout: 20000})
    cy.get('app-generic-landing').should('not.exist')
    cy.get('.side-bar').contains("Public details")
    cy.get('.side-bar').contains("Website")
    cy.get('.side-bar').contains("Email")
    cy.get('.side-bar').contains("No website added")
    cy.get('.side-bar').contains("No email added")
    cy.get('.main-section').contains("Consortium lead")
    cy.get('.main-section').contains("Mambo No 5")
    cy.get('.main-section').contains("Consortium Members (4)")
    cy.get('.main-section').contains("Member name")
    cy.get('.main-section').contains("Almonds Forest")
    cy.get('.main-section').contains('Contacts')
    cy.get('.main-section').contains('Main relationship contact (OFFICIAL)')
    cy.get('.main-section').contains('first.last@orcid.org')
  });

  it('Consortium member', function() {
    cy.programmaticSignin(data.homepageTestMembers.consortiumMember.email, credentials.password);
    cy.visit('/');
    cy.get('app-member-info-landing', {timeout: 20000})
    cy.get('app-generic-landing').should('not.exist')
    cy.get('.side-bar').contains("Public details")
    cy.get('.side-bar').contains("Website")
    cy.get('.side-bar').contains("Email")
    cy.get('.side-bar').contains("No website added")
    cy.get('.side-bar').contains("@orcid.org")
    cy.get('.main-section').contains("Consortium/Parent organization: Mambo No 5")
    cy.get('.main-section').contains("Membership: Active")
    cy.get('.main-section').contains("Almonds Forest")
    cy.get('.main-section').contains("Description")
    cy.get('.main-section').contains('Contacts')
    cy.get('.main-section').contains('Agreement signatory (OFFICIAL)')
    cy.get('.main-section').contains('last@orcid.org')
  });

  it('Consortium member 2', function() {
    cy.programmaticSignin(data.homepageTestMembers.consortiumMemberEmail2, credentials.password);
    cy.visit('/');
    cy.get('app-member-info-landing', {timeout: 20000})
    cy.get('app-generic-landing').should('not.exist')
    cy.get('.side-bar').contains("Public details")
    cy.get('.side-bar').contains("Website")
    cy.get('.side-bar').contains("Email")
    cy.get('.side-bar').contains("canadapost.ca")
    cy.get('.side-bar').contains("support@orcid.org")
    cy.get('.main-section').contains("Consortium/Parent organization: The Concord of Kinship")
    cy.get('.main-section').contains("Membership: Active")
    cy.get('.main-section').contains("Grateful Frogs")
  });

  it('Consortium member and lead', function() {
    cy.programmaticSignin(data.homepageTestMembers.consortiumLeadAndMember.email, credentials.password);
    cy.visit('/');
    cy.get('app-member-info-landing', {timeout: 20000})
    cy.get('app-generic-landing').should('not.exist')
    cy.get('.side-bar').contains("Public details")
    cy.get('.side-bar').contains("Website")
    cy.get('.side-bar').contains("Email")
    cy.get('.side-bar').contains("www.haevesting.com")
    cy.get('.side-bar').contains("hhh@hhh.com")
    cy.get('.main-section').contains("Consortium lead")
    cy.get('.main-section').contains("Consortium Members (2)")
    cy.get('.main-section').contains("Member name")
    cy.get('.main-section').contains("Yellow member")
    cy.get('.main-section').contains("The Harvest Ascendancy")
    cy.get('.main-section').contains('Contacts')
    cy.get('.main-section').contains('Product Contact')
    cy.get('.main-section').contains('testingagain@orcid.org')
  });

  it('Inactive member', function() {
    cy.programmaticSignin(data.homepageTestMembers.inactiveConsortiumMemberEmail, credentials.password);
    cy.visit('/');
    cy.get('app-generic-landing', {timeout: 20000}).contains('Something has gone wrong...')
    cy.get('app-member-info-landing').should('not.exist')
  });
});
