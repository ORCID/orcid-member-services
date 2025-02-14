// ***********************************************
// This example commands.js shows you how to
// create various custom commands and overwrite
// existing commands.
//
// For more comprehensive examples of custom
// commands please read more here:
// https://on.cypress.io/custom-commands
// ***********************************************
//
//
// -- This is a parent command --
// Cypress.Commands.add('login', (email, password) => { ... })
//
//
// -- This is a child command --
// Cypress.Commands.add('drag', { prevSubject: 'element'}, (subject, options) => { ... })
//
//
// -- This is a dual command --
// Cypress.Commands.add('dismiss', { prevSubject: 'optional'}, (subject, options) => { ... })
//
//
// -- This will overwrite an existing command --
// Cypress.Commands.overwrite('visit', (originalFn, url, options) => { ... })
import "cypress-file-upload";
import data from "../fixtures/test-data.json";
import credentials from "../fixtures/credentials.json";
import record from "../fixtures/orcid-record.json";

Cypress.Commands.add("signin", (email, password) => {
  cy.visit("en/");
  cy.get("#username")
    .clear()
    .type(email)
    .get("#password")
    .type(password)
    .get("button")
    .filter('[type="submit"]')
    .click();
  cy.get('[data-cy="signedInMessage"]').should("exist");
});

Cypress.Commands.add("checkOrgId", (org, invalidId, id) => {
  cy.get("#field_disambiguationSource").select(org);
  cy.get("small.text-danger").should("exist");
  cy.get("#field_disambiguatedOrgId").clear().type(invalidId);
  cy.get("small.text-danger").should("exist");
  cy.get("#field_disambiguatedOrgId").clear().type(id);
  cy.get("small.text-danger").should("not.exist");
});

Cypress.Commands.add("programmaticSignin", (username, password) => {
  cy.getCookie("XSRF-TOKEN").then((csrfCookie) => {
    if (!csrfCookie) {
      return cy
        .visit("en/")
        .getCookie("XSRF-TOKEN")
        .then(() => cy.programmaticSignin(username, password));
    } else {
      cy.log(csrfCookie.value);
      cy.request({
        method: "POST",
        url: "/auth/login",
        headers: { "X-XSRF-TOKEN": csrfCookie.value },
        failOnStatusCode: false, // dont fail so we can make assertions
        body: {
          username,
          password,
        },
      }).then((r) => {
        if (r.status != 200) {
          cy.signin(username, password);
        }
      });
    }
  });
});

Cypress.Commands.add("programmaticSignout", () => {
  cy.getCookie("XSRF-TOKEN").then((csrfCookie) => {
    cy.log(csrfCookie.value);
    cy.request({
      method: "POST",
      url: "/auth/logout",
      headers: { "X-XSRF-TOKEN": csrfCookie.value },
      failOnStatusCode: false, // dont fail so we can make assertions
    }).then((r) => {
      cy.log(r);
      // expect(r.status).to.eq(204);
    });
  });
});

Cypress.Commands.add("processPasswordForm", (newPasswordFieldId) => {
  cy.get("button")
    .filter('[type="submit"]')
    // make sure you can't activate account without providing a password
    .invoke("attr", "disabled")
    .should("exist");
  // type invalid passwords
  cy.get(newPasswordFieldId).clear().type(credentials.shortPassword);
  cy.get("#confirmPassword")
    .clear()
    .type(credentials.shortConfirmationPassword);
  // check for min length error messages
  cy.get("small").filter('[data-cy="passwordTooShort"]').should("exist");
  cy.get("small")
    .filter('[data-cy="confirmationPasswordTooShort"]')
    .should("exist");
  // fix password
  cy.get(newPasswordFieldId).clear().type(credentials.password);
  // enter invalid confirmation password
  cy.get("#confirmPassword")
    .clear()
    .type(credentials.wrongConfirmationPasssword);
  // make sure you can't activate account
  cy.get("button").filter('[type="submit"]').click();
  // check for confirmation error message
  cy.get("div").filter('[data-cy="passwordsDoNotMatch"]').should("exist");
  // fix confirmation password
  cy.get("#confirmPassword").clear().type(credentials.password);
  // activate account
  cy.get("button").filter('[type="submit"]').click();
});

Cypress.Commands.add("visitLinkFromEmail", (email) => {
  const emailBody = email[0].body.html;
  assert.isNotNull(emailBody);
  cy.log(">>>>>>>>>Email body is: " + JSON.stringify(email.body));
  //convert string to DOM
  const htmlDom = new DOMParser().parseFromString(emailBody, "text/html");
  //href points to correct endpoint
  const href = htmlDom.querySelector(
    'a[href*="https://member-portal.qa.orcid.org/en/reset/finish?key="]',
  ).href;
  cy.visit(href);
});

Cypress.Commands.add("checkInbox", (subject, recipient, date) => {
  cy.task("checkInbox", {
    options: {
      from: data.outbox.email,
      to: recipient,
      subject,
      include_body: true,
      after: date,
    },
  });
});

Cypress.Commands.add("removeAffiliation", ($e) => {
  cy.wrap($e).children().last().click();
  cy.get("#jhi-confirm-delete-msUser").click();
});

Cypress.Commands.add("changeOrgOwner", () => {
  cy.visit(`en/users/${data.member.users.owner.id}/edit`);
  cy.get("#field_mainContact").click();
  cy.get("#save-entity").click();
  cy.get(".alert-success").should("exist");
  cy.programmaticSignout();
});

Cypress.Commands.add("readCsv", (data) => {
  var lines = data.split("\n");
  var result = [];
  var headers = lines[0].split(",");
  for (var i = 1; i < lines.length; i++) {
    var obj = {};
    var currentline = lines[i].split(",");

    for (var j = 0; j < headers.length; j++) {
      obj[headers[j]] = currentline[j];
    }
    result.push(obj);
  }
  return result;
});

Cypress.Commands.add("uploadCsv", (path) => {
  cy.get("#jh-upload-entities").click();
  cy.get("#field_filePath").attachFile(path);
  cy.intercept(
    "https://member-portal.qa.orcid.org/services/assertionservice/api/assertion/upload",
  ).as("upload");
  cy.get("button").filter('[data-cy="confirmCsvUpload"]').click();
  // Occasionally, trying to upload the csv results in a 403 code due to an invalid CSRF token, in which case we retry
  cy.wait("@upload").then((int) => {
    if (int.response.statusCode !== 200) {
      y.get(button).filter('[data-cy="confirmCsvUpload"]').click();
    }
  });
});

Cypress.Commands.add("fetchLinkAndGrantPermission", (email) => {
  // get permission link from first affiliation in the list
  cy.get("tbody")
    .children()
    .last()
    .within(() => {
      cy.get("a").click();
    });
  cy.get("button").filter('[data-cy="copyToClipboard"]').click(),
    cy.task("getClipboard").then((link) => {
      cy.visit(link);
    });
  // Handle cookies
  cy.get("#onetrust-reject-all-handler").click();
  // Grant permission
  cy.get("#username-input").clear().type(email);
  cy.get("#password").type(credentials.password);
  cy.get("#signin-button").click();

  // *ADD ID
  cy.get(".mt-5").within(() => {
    cy.get("h2").filter('[data-cy="thanksMessage"]').should("exist");
  });
});

Cypress.Commands.add("checkAffiliationChanges", (affiliation, value) => {
  expect(affiliation["department-name"]).to.eq(value);
  expect(affiliation["role-title"]).to.eq(value);
  expect(affiliation["organization"]["address"]["city"]).to.eq(value);
  expect(affiliation["organization"]["name"]).to.eq(value);
});

/** ******************************************************************************
 * *******************************************************************************
 *
 * SECURITY TESTS
 *
 * *******************************************************************************
 * *******************************************************************************/

/** ****
 * *****
 *
 * USERS
 *
 * *****
 * *****/

Cypress.Commands.add("getUsersBySfId", (salesforceId, code, title) => {
  cy.log("getUsersBySfId");
  cy.request({
    url: `/services/userservice/api/users/salesforce/${salesforceId}`,
    failOnStatusCode: false,
  }).then((resp) => {
    expect(resp.status).to.eq(code);
    expect(resp.body.title).to.eq(title);
  });
});

Cypress.Commands.add("getAllUsers", (code, title) => {
  cy.log("getAllUsers");
  cy.request({
    url: "/services/userservice/api/users/",
    failOnStatusCode: false,
  }).then((resp) => {
    expect(resp.status).to.eq(code);
    expect(resp.body.title).to.eq(title);
  });
});

/** ******
 * *******
 *
 * MEMBERS
 *
 * *******
 * *******/

Cypress.Commands.add("getAllMembers", (code, title) => {
  cy.log("getAllMembers");
  cy.request({
    url: "/services/memberservice/api/members",
    failOnStatusCode: false,
  }).then((resp) => {
    expect(resp.status).to.eq(code);
    expect(resp.body.title).to.eq(title);
  });
});

Cypress.Commands.add(
  "addMember",
  (salesforceId, assertionServiceEnabled, isConsortiumLead, code, title) => {
    cy.log("addMember");
    cy.getCookie("XSRF-TOKEN").then((csrfCookie) => {
      cy.request({
        method: "POST",
        url: "/services/memberservice/api/members",
        headers: { "X-XSRF-TOKEN": csrfCookie.value },
        failOnStatusCode: false,
        body: {
          salesforceId,
          assertionServiceEnabled,
          isConsortiumLead,
        },
      }).then((resp) => {
        expect(resp.status).to.eq(code);
        expect(resp.body.title).to.eq(title);
      });
    });
  },
);

Cypress.Commands.add(
  "updateMember",
  (salesforceId, assertionServiceEnabled, isConsortiumLead, code, title) => {
    cy.log("updateMember");
    cy.getCookie("XSRF-TOKEN").then((csrfCookie) => {
      cy.request({
        method: "PUT",
        url: "/services/memberservice/api/members",
        headers: { "X-XSRF-TOKEN": csrfCookie.value },
        failOnStatusCode: false,
        body: {
          salesforceId,
          assertionServiceEnabled,
          isConsortiumLead,
        },
      }).then((resp) => {
        expect(resp.status).to.eq(code);
        expect(resp.body.title).to.eq(title);
      });
    });
  },
);

Cypress.Commands.add(
  "validateMember",
  (salesforceId, assertionServiceEnabled, isConsortiumLead, code, title) => {
    cy.log("validateMember");
    cy.getCookie("XSRF-TOKEN").then((csrfCookie) => {
      cy.request({
        method: "POST",
        url: "/services/memberservice/api/members/validate",
        headers: { "X-XSRF-TOKEN": csrfCookie.value },
        failOnStatusCode: false,
        body: {
          salesforceId,
          assertionServiceEnabled,
          isConsortiumLead,
        },
      }).then((resp) => {
        expect(resp.status).to.eq(code);
        expect(resp.body.title).to.eq(title);
      });
    });
  },
);

Cypress.Commands.add(
  "changeNotificationLanguage",
  (salesforceId, language, code, title) => {
    cy.log("changeNotificationLanguage");
    cy.getCookie("XSRF-TOKEN").then((csrfCookie) => {
      cy.request({
        method: "POST",
        url: `/services/memberservice/api/members/${salesforceId}/language/${language}`,
        headers: { "X-XSRF-TOKEN": csrfCookie.value },
        failOnStatusCode: false,
      }).then((resp) => {
        expect(resp.status).to.eq(code);
        expect(resp.body.title).to.eq(title);
      });
    });
  },
);

Cypress.Commands.add("updateContact", (salesforceId, code, title) => {
  cy.log("updateContact");
  cy.getCookie("XSRF-TOKEN").then((csrfCookie) => {
    cy.request({
      method: "POST",
      url: `/services/memberservice/api/members/${salesforceId}/contact-update`,
      headers: { "X-XSRF-TOKEN": csrfCookie.value },
      failOnStatusCode: false,
      body: {},
    }).then((resp) => {
      expect(resp.status).to.eq(code);
      expect(resp.statusText).to.eq(title);
    });
  });
});

Cypress.Commands.add(
  "updateMemberDetails",
  (salesforceId, publicName, code, title) => {
    cy.log("updateMemberDetails");
    cy.getCookie("XSRF-TOKEN").then((csrfCookie) => {
      cy.request({
        method: "PUT",
        url: `/services/memberservice/api/members/${salesforceId}/member-details`,
        headers: { "X-XSRF-TOKEN": csrfCookie.value },
        failOnStatusCode: false,
        body: {
          publicName,
          salesforceId,
        },
      }).then((resp) => {
        expect(resp.status).to.eq(code);
        expect(resp.statusText).to.eq(title);
      });
    });
  },
);

Cypress.Commands.add("getMemberDetails", (salesforceId, code, title) => {
  cy.log("getMemberDetails");
  cy.request({
    url: `/services/memberservice/api/members/${salesforceId}/member-details`,
    failOnStatusCode: false,
  }).then((resp) => {
    expect(resp.status).to.eq(code);
    expect(resp.statusText).to.eq(title);
  });
});

Cypress.Commands.add("getMemberContacts", (salesforceId, code, title) => {
  cy.log("getMemberContacts");
  cy.request({
    url: `/services/memberservice/api/members/${salesforceId}/member-contacts`,
    failOnStatusCode: false,
  }).then((resp) => {
    expect(resp.status).to.eq(code);
    expect(resp.statusText).to.eq(title);
  });
});

Cypress.Commands.add("getMemberOrgIds", (salesforceId, code, title) => {
  cy.log("getMemberOrgIds");
  cy.request({
    url: `/services/memberservice/api/members/${salesforceId}/member-org-ids`,
    failOnStatusCode: false,
  }).then((resp) => {
    expect(resp.status).to.eq(code);
    expect(resp.statusText).to.eq(title);
  });
});

Cypress.Commands.add("getMembersList", (code, title) => {
  cy.log("getMemberList");
  cy.request({
    url: "/services/memberservice/api/members/list/all",
    failOnStatusCode: false,
  }).then((resp) => {
    expect(resp.status).to.eq(code);
    expect(resp.body.title).to.eq(title);
  });
});

Cypress.Commands.add("getMember", (salesforceId, code, title) => {
  cy.log("getMember");
  cy.request({
    url: `/services/memberservice/api/members/${salesforceId}`,
    failOnStatusCode: false,
  }).then((resp) => {
    expect(resp.status).to.eq(code);
    expect(resp.body.title).to.eq(title);
  });
});

Cypress.Commands.add("deleteMember", (salesforceId, code, title) => {
  cy.log("deleteMember");
  cy.getCookie("XSRF-TOKEN").then((csrfCookie) => {
    cy.request({
      method: "DELETE",
      url: `/services/memberservice/api/members/${salesforceId}`,
      headers: { "X-XSRF-TOKEN": csrfCookie.value },
      failOnStatusCode: false,
    }).then((resp) => {
      expect(resp.status).to.eq(code);
      expect(resp.body.title).to.eq(title);
    });
  });
});

Cypress.Commands.add("addConsortiumMember", (code, title) => {
  cy.log("addConsortiumMember");
  cy.getCookie("XSRF-TOKEN").then((csrfCookie) => {
    cy.request({
      method: "POST",
      url: `/services/memberservice/api/members/add-consortium-member`,
      headers: { "X-XSRF-TOKEN": csrfCookie.value },
      failOnStatusCode: false,
      body: {},
    }).then((resp) => {
      expect(resp.status).to.eq(code);
      expect(resp.body?.title).to.eq(title);
    });
  });
});

Cypress.Commands.add("removeConsortiumMember", (code, title) => {
  cy.log("removeConsortiumMember");
  cy.getCookie("XSRF-TOKEN").then((csrfCookie) => {
    cy.request({
      method: "POST",
      url: `/services/memberservice/api/members/remove-consortium-member`,
      headers: { "X-XSRF-TOKEN": csrfCookie.value },
      failOnStatusCode: false,
      body: {},
    }).then((resp) => {
      expect(resp.status).to.eq(code);
      expect(resp.body?.title).to.eq(title);
    });
  });
});

/** *********
 * **********
 *
 * ASSERTIONS
 *
 * **********
 * **********/

Cypress.Commands.add("getAssertions", (code, title) => {
  cy.log("getAssertions");
  cy.request({
    url: "/services/assertionservice/api/assertions",
    failOnStatusCode: false,
  }).then((resp) => {
    expect(resp.status).to.eq(code);
    expect(resp.body.title).to.eq(title);
  });
});
