/// <reference types="cypress" />
import data from "../../fixtures/test-data.json";
import credentials from "../../fixtures/credentials.json";

describe("Test restricted access", () => {
  afterEach(() => {
    cy.programmaticSignout();
  });

  it("Regular users should not be able to access the Manage Members menu", function () {
    cy.programmaticSignin(
      data.populatedMember.users.user.email,
      credentials.password,
    );
    cy.visit("en/members");
    cy.get("h1")
      .filter('[data-cy="cannotProcessRequest"]')
      .contains("Your request cannot be processed");
    cy.get("div")
      .filter('[data-cy="notAuthorized"]')
      .invoke("attr", "hidden")
      .should("not.exist");
  });

  it("Regular users should not be able to access the Manage Users menu", function () {
    cy.programmaticSignin(
      data.populatedMember.users.user.email,
      credentials.password,
    );
    cy.visit("en/users");
    cy.get("h1")
      .filter('[data-cy="cannotProcessRequest"]')
      .contains("Your request cannot be processed");
    cy.get("div")
      .filter('[data-cy="notAuthorized"]')
      .invoke("attr", "hidden")
      .should("not.exist");
  });

  it("Org owners should not be able to access the Manage Members menu", function () {
    cy.programmaticSignin(
      data.populatedMember.users.owner.email,
      credentials.password,
    );
    cy.visit("en/members");
    cy.get("h1")
      .filter('[data-cy="cannotProcessRequest"]')
      .contains("Your request cannot be processed");
    cy.get("div")
      .filter('[data-cy="notAuthorized"]')
      .invoke("attr", "hidden")
      .should("not.exist");
  });

  it("Cannot access the assertion menu if assertions are not enabled", function () {
    cy.programmaticSignin(
      data.homepageTestMembers.consortiumLeadAndMember.email,
      credentials.password,
    );
    cy.visit("en/affiliations");
    cy.get("h1")
      .filter('[data-cy="cannotProcessRequest"]')
      .contains("Your request cannot be processed");
    cy.get("div")
      .filter('[data-cy="notAuthorized"]')
      .invoke("attr", "hidden")
      .should("not.exist");
  });
});
