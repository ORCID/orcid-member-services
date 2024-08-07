/// <reference types="cypress" />
import data from "../../fixtures/test-data.json";
import credentials from "../../fixtures/credentials.json";

describe("Test report menus", () => {
  it("Direct member", function () {
    cy.programmaticSignin(
      data.homepageTestMembers.directMemberEmail,
      credentials.password,
    );
    cy.visit("en/");
    cy.get("app-member-info", { timeout: 20000 });
    cy.get("#tools-menu").click();
    cy.get('[routerLink="/report/member"]').should("be.visible");
    cy.get('[routerLink="/report/integration"]').should("be.visible");
    cy.get('[routerLink="/report/affiliation"]').should("be.visible");
    cy.get('[routerLink="/report/consortia"]').should("not.exist");
    cy.get('[routerLink="/report/consortia-member-affiliations"]').should(
      "not.exist",
    );
    cy.request({
      url: "/services/memberservice/api/reports/member",
      failOnStatusCode: false,
    }).then((resp) => {
      expect(resp.status).to.eq(200);
    });
    cy.request({
      url: "/services/memberservice/api/reports/integration",
      failOnStatusCode: false,
    }).then((resp) => {
      expect(resp.status).to.eq(200);
    });
    cy.request({
      url: "/services/memberservice/api/reports/affiliation",
      failOnStatusCode: false,
    }).then((resp) => {
      expect(resp.status).to.eq(200);
    });
    cy.request({
      url: "/services/memberservice/api/reports/consortia",
      failOnStatusCode: false,
    }).then((resp) => {
      expect(resp.status).to.eq(400);
    });
    cy.request({
      url: "/services/memberservice/api/reports/consortia-member-affiliations",
      failOnStatusCode: false,
    }).then((resp) => {
      expect(resp.status).to.eq(400);
    });
  });

  it("Consortium lead", function () {
    cy.programmaticSignin(
      data.homepageTestMembers.consortiumLeadEmail,
      credentials.password,
    );
    cy.visit("en/");
    cy.get("#tools-menu").click();
    cy.get('[routerlink="/report/member"]').should("be.visible");
    cy.get('[routerlink="/report/integration"]').should("be.visible");
    cy.get('[routerlink="/report/affiliation"]').should("be.visible");
    cy.get('[routerlink="/report/consortia"]').should("be.visible");
    cy.get('[routerlink="/report/consortia-member-affiliations"]').should(
      "be.visible",
    );
    cy.request({
      url: "/services/memberservice/api/reports/member",
      failOnStatusCode: false,
    }).then((resp) => {
      expect(resp.status).to.eq(200);
    });
    cy.request({
      url: "/services/memberservice/api/reports/integration",
      failOnStatusCode: false,
    }).then((resp) => {
      expect(resp.status).to.eq(200);
    });
    cy.request({
      url: "/services/memberservice/api/reports/affiliation",
      failOnStatusCode: false,
    }).then((resp) => {
      expect(resp.status).to.eq(200);
    });
    cy.request({
      url: "/services/memberservice/api/reports/consortia",
      failOnStatusCode: false,
    }).then((resp) => {
      expect(resp.status).to.eq(200);
    });
    cy.request({
      url: "/services/memberservice/api/reports/consortia-member-affiliations",
      failOnStatusCode: false,
    }).then((resp) => {
      expect(resp.status).to.eq(200);
    });
  });

  it("Consortium member", function () {
    cy.programmaticSignin(
      data.homepageTestMembers.consortiumMember.email,
      credentials.password,
    );
    cy.visit("en/");
    cy.get("#tools-menu").click();
    cy.get('[routerlink="/report/member"]').should("be.visible");
    cy.get('[routerlink="/report/integration"]').should("be.visible");
    cy.get('[routerlink="/report/affiliation"]').should("be.visible");
    cy.get('[routerlink="/report/consortia"]').should("not.exist");
    cy.get('[routerlink="/report/consortia-member-affiliations"]').should(
      "not.exist",
    );
    cy.request({
      url: "/services/memberservice/api/reports/member",
      failOnStatusCode: false,
    }).then((resp) => {
      expect(resp.status).to.eq(200);
    });
    cy.request({
      url: "/services/memberservice/api/reports/integration",
      failOnStatusCode: false,
    }).then((resp) => {
      expect(resp.status).to.eq(200);
    });
    cy.request({
      url: "/services/memberservice/api/reports/affiliation",
      failOnStatusCode: false,
    }).then((resp) => {
      expect(resp.status).to.eq(200);
    });
    cy.request({
      url: "/services/memberservice/api/reports/consortia",
      failOnStatusCode: false,
    }).then((resp) => {
      expect(resp.status).to.eq(400);
    });
    cy.request({
      url: "/services/memberservice/api/reports/consortia-member-affiliations",
      failOnStatusCode: false,
    }).then((resp) => {
      expect(resp.status).to.eq(400);
    });
  });

  it("Consortium member 2", function () {
    cy.programmaticSignin(
      data.homepageTestMembers.consortiumMemberEmail2,
      credentials.password,
    );
    cy.visit("en/");
    cy.get("#tools-menu").click();
    cy.get('[routerlink="/report/member"]').should("be.visible");
    cy.get('[routerlink="/report/integration"]').should("be.visible");
    cy.get('[routerlink="/report/affiliation"]').should("be.visible");
    cy.get('[routerlink="/report/consortia"]').should("not.exist");
    cy.get('[routerlink="/report/consortia-member-affiliations"]').should(
      "not.exist",
    );
    cy.request({
      url: "/services/memberservice/api/reports/member",
      failOnStatusCode: false,
    }).then((resp) => {
      expect(resp.status).to.eq(200);
    });
    cy.request({
      url: "/services/memberservice/api/reports/integration",
      failOnStatusCode: false,
    }).then((resp) => {
      expect(resp.status).to.eq(200);
    });
    cy.request({
      url: "/services/memberservice/api/reports/affiliation",
      failOnStatusCode: false,
    }).then((resp) => {
      expect(resp.status).to.eq(200);
    });
    cy.request({
      url: "/services/memberservice/api/reports/consortia",
      failOnStatusCode: false,
    }).then((resp) => {
      expect(resp.status).to.eq(400);
    });
    cy.request({
      url: "/services/memberservice/api/reports/consortia-member-affiliations",
      failOnStatusCode: false,
    }).then((resp) => {
      expect(resp.status).to.eq(400);
    });
  });

  it("Consortium member and lead", function () {
    cy.programmaticSignin(
      data.homepageTestMembers.consortiumLeadAndMember.email,
      credentials.password,
    );
    cy.visit("en/");
    cy.get("#tools-menu").click();
    cy.get('[routerlink="/report/member"]').should("be.visible");
    cy.get('[routerlink="/report/integration"]').should("be.visible");
    cy.get('[routerlink="/report/affiliation"]').should("be.visible");
    cy.get('[routerlink="/report/consortia"]').should("be.visible");
    cy.get('[routerlink="/report/consortia-member-affiliations"]').should(
      "be.visible",
    );
    cy.request({
      url: "/services/memberservice/api/reports/member",
      failOnStatusCode: false,
    }).then((resp) => {
      expect(resp.status).to.eq(200);
    });
    cy.request({
      url: "/services/memberservice/api/reports/integration",
      failOnStatusCode: false,
    }).then((resp) => {
      expect(resp.status).to.eq(200);
    });
    cy.request({
      url: "/services/memberservice/api/reports/affiliation",
      failOnStatusCode: false,
    }).then((resp) => {
      expect(resp.status).to.eq(200);
    });
    cy.request({
      url: "/services/memberservice/api/reports/consortia",
      failOnStatusCode: false,
    }).then((resp) => {
      expect(resp.status).to.eq(200);
    });
    cy.request({
      url: "/services/memberservice/api/reports/consortia-member-affiliations",
      failOnStatusCode: false,
    }).then((resp) => {
      expect(resp.status).to.eq(200);
    });
  });

  it("Inactive member", function () {
    cy.programmaticSignin(
      data.homepageTestMembers.inactiveConsortiumMemberEmail,
      credentials.password,
    );
    cy.visit("en/");
    cy.get("#tools-menu").click();
    cy.get('[routerLink="/report/member"]').should("be.visible");
    cy.get('[routerLink="/report/integration"]').should("be.visible");
    cy.get('[routerLink="/report/affiliation"]').should("be.visible");
    cy.get('[routerLink="/report/consortia"]').should("not.exist");
    cy.get('[routerLink="/report/consortia-member-affiliations"]').should(
      "not.exist",
    );
    cy.request({
      url: "/services/memberservice/api/reports/member",
      failOnStatusCode: false,
    }).then((resp) => {
      expect(resp.status).to.eq(200);
    });
    cy.request({
      url: "/services/memberservice/api/reports/integration",
      failOnStatusCode: false,
    }).then((resp) => {
      expect(resp.status).to.eq(200);
    });
    cy.request({
      url: "/services/memberservice/api/reports/affiliation",
      failOnStatusCode: false,
    }).then((resp) => {
      expect(resp.status).to.eq(200);
    });
    cy.request({
      url: "/services/memberservice/api/reports/consortia",
      failOnStatusCode: false,
    }).then((resp) => {
      expect(resp.status).to.eq(400);
    });
    cy.request({
      url: "/services/memberservice/api/reports/consortia-member-affiliations",
      failOnStatusCode: false,
    }).then((resp) => {
      expect(resp.status).to.eq(400);
    });
  });
});
