import { defineConfig } from 'cypress';

export default defineConfig({
  taskTimeout: 300000,
  defaultCommandTimeout: 10000,
  video: false,
  e2e: {
    // We've imported your old cypress plugins here.
    // You may want to clean this up later by importing these.
    setupNodeEvents(on, config) {
      return require('./cypress/plugins/index.js')(on, config);
    },
    // TODO: cy.visit('ui') should be changed to cy.visit('/')
    baseUrl: 'https://member-portal.qa.orcid.org',
  },
});
