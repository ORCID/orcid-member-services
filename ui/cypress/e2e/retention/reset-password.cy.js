/// <reference types="cypress" />
import data from "../../fixtures/test-data.json";
import credentials from "../../fixtures/credentials.json";

describe("Test the password reset functionality", () => {
  it("Forgot your password?", function () {
    cy.visit("ui/en/reset/request");
    cy.get("#email").type(data.invalidEmail);
    cy.get("small").filter('[data-cy="emailInvalid"]').should("exist");
    cy.get("button")
      .filter('[type="submit"]')
      .invoke("attr", "disabled")
      .should("exist");
    cy.get("#email").clear().type(data.member.users.owner.email);
    cy.get("button").filter('[type="submit"]').click();

    cy.task("checkInbox", {
      to: data.member.users.owner.email,
      subject: data.outbox.resetPasswordSubject,
    }).then((email) => {
      cy.visitLinkFromEmail(email);
    });

    cy.processPasswordForm("#password");

    cy.get(".alert-success").within(() => {
      cy.get("a").filter('data-cy="navigateToSignIn"]').click();
    });
    // sign in and confirm the activation was successful
    cy.programmaticSignin(data.member.users.owner.email, credentials.password);
    cy.programmaticSignout();
  });

  it("Change password", function () {
    cy.programmaticSignin(
      data.populatedMember.users.owner.email,
      credentials.password,
    );
    cy.visit("ui/en/password");
    cy.get("#currentPassword").type(credentials.wrongConfirmationPasssword);
    cy.processPasswordForm("#newPassword");
    cy.get(".alert-danger")
      .filter('data-cy="passwordChangeError"]')
      .should("exist");
    cy.get("#currentPassword").clear().type(credentials.password);
    cy.get("button").filter('[type="submit"]').click();
    cy.get(".alert-success").should("exist");
  });
});
