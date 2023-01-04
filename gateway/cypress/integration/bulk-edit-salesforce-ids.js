/// <reference types="cypress" />
import config from '../fixtures/bulk-edit-salesforce-ids-config.json';

describe('Bulk edit invalid salesforce ids', () => {
  it('Replace all invalid ids with unused valid ones', () => {
    cy.request({
      url: config.endpoint,
      method: 'GET',
      headers: {
        Authorization: config.token
      }
    }).then(res => {
      let totalPages = 0;
      let sfIds = [];
      let mpIds = [];
      let data = res.body;
      data = JSON.parse(data);
      for (const id in data.records) {
        if (!sfIds.includes(data.records[id]['Id'])) {
          sfIds.push(data.records[id]['Id']);
        }
      }
      cy.visit(config.base_url);
      cy.get('#username')
        .clear()
        .type(config.username)
        .get('#password')
        .type(config.password)
        .get('button')
        .filter('[type="submit"]')
        .click();
      cy.get('#admin-menu').click();
      cy.get('a')
        .filter('[href="/member"]')
        .click();
      cy.wait(1000);
      cy.get('.pagination')
        .children()
        .eq(-3)
        .then(e => {
          totalPages = +e[0].textContent.replace('(current)', '').trim();
          // get list of salesforce ids used in the portal
          for (let i = 0; i < totalPages; i++) {
            cy.get('tbody')
              .children()
              .each($e => {
                mpIds.push($e[0].children[0].textContent);
              });
            cy.get('.pagination')
              .children()
              .eq(-2)
              .click();
          }
        })
        .then(() => {
          // create list of salesforce ids not used in the portal
          let filteredIds = [];
          for (var i = 0; i < sfIds.length; i += 1) {
            if (mpIds.indexOf(sfIds[i]) == -1) {
              filteredIds.push(sfIds[i]);
            }
          }
          // go back to first page
          cy.get('.pagination')
            .children()
            .eq(0)
            .click();
          cy.wait(1000);
          for (var i = 0; i < totalPages; i++) {
            cy.get('.pagination > li.active').then(page => {
              // get current page number
              const pageNumber = +page[0].textContent.replace('(current)', '').trim();
              cy.get('tbody')
                .children()
                .each($e => {
                  if ($e[0].children[0].textContent !== '001G000001AP83e') {
                    if (!sfIds.includes($e[0].children[0].textContent)) {
                      // click on the edit button
                      cy.get('tbody')
                        .children()
                        .eq($e[0].sectionRowIndex)
                        .children()
                        .eq(6)
                        .children()
                        .eq(0)
                        .click();
                        // change id
                      cy.get('#field_salesforceId')
                        .clear()
                        .type(filteredIds[0]);
                      cy.get('#save-entity').click();
                      filteredIds.shift()
                      // saving the id redirects you to page 1, make sure you return to the relevant page
                      if (pageNumber !== 1) {
                        for (var p = 1; p < pageNumber; p++) {
                          cy.get('.pagination')
                            .children()
                            .eq(-2)
                            .click();
                        }
                        cy.wait(100);
                      }
                    }
                  }
                });
              // go to the next page
              cy.get('.pagination')
                .children()
                .eq(-2)
                .click();
              cy.wait(250);
            });
          }
        });
    });
  });
});
