/// <reference types="cypress" />
import data from '../../fixtures/test-data.json';
import credentials from '../../fixtures/credentials.json';

describe('Test homepage', () => {
  it('Direct member', function() {
    cy.programmaticSignin(data.homepageTestMembers.directMemberEmail, credentials.password);
    cy.visit('/');
    cy.get('app-member-info-landing')
    cy.get('app-generic-landing').should('not.exist')
    cy.get('.side-bar').contains("Public details")
    cy.get('.side-bar').contains("Website")
    cy.get('.side-bar').contains("Email")
    cy.get('.side-bar').contains("https://orcid.org")
    cy.get('.side-bar').contains("orcid@orcid.org")
    cy.get('.main-section').contains("Membership: Active")
    cy.get('.main-section').contains('Fly and Mighty')
  });

  it('Consortium lead', function() {
    cy.programmaticSignin(data.homepageTestMembers.consortiumLeadEmail, credentials.password);
    cy.visit('/');
    cy.get('app-member-info-landing')
    cy.get('app-generic-landing').should('not.exist')
    cy.get('.side-bar').contains("Public details")
    cy.get('.side-bar').contains("Website")
    cy.get('.side-bar').contains("Email")
    cy.get('.side-bar').contains("https://www.testtest.com")
    cy.get('.side-bar').contains("No email added")
    cy.get('.main-section').contains("Consortium lead")
    cy.get('.main-section').contains("Mambo No 5")
    cy.get('.main-section').contains("Consortium Members (1)")
    cy.get('.main-section').contains("Member name")
    cy.get('.main-section').contains("Almonds Forest")
  });

  it('Consortium member', function() {
    cy.programmaticSignin(data.homepageTestMembers.consortiumMemberEmail, credentials.password);
    cy.visit('/');
    cy.get('app-member-info-landing')
    cy.get('app-generic-landing').should('not.exist')
    cy.get('.side-bar').contains("Public details")
    cy.get('.side-bar').contains("Website")
    cy.get('.side-bar').contains("Email")
    cy.get('.side-bar').contains("https://orcid.org")
    cy.get('.side-bar').contains("almondsforest@orcid.org")
    cy.get('.main-section').contains("Consortium/Parent organization: Mambo No 5")
    cy.get('.main-section').contains("Membership: Active")
    cy.get('.main-section').contains("Almonds Forest")
  });

  it('Consortium member 2', function() {
    cy.programmaticSignin(data.homepageTestMembers.consortiumMemberEmail2, credentials.password);
    cy.visit('/');
    cy.get('app-member-info-landing')
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
    cy.programmaticSignin(data.homepageTestMembers.consortiumLeadAndMemberEmail, credentials.password);
    cy.visit('/');
    cy.get('app-member-info-landing')
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
  });

  it('Inactive member', function() {
    cy.programmaticSignin(data.homepageTestMembers.inactiveConsortiumMemberEmail, credentials.password);
    cy.visit('/');
    cy.get('app-generic-landing').contains('Something has gone wrong...')
    cy.get('app-member-info-landing').should('not.exist')
  });
});
