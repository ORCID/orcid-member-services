/// <reference types="cypress" />
// ***********************************************************
// This example plugins/index.js can be used to load plugins
//
// You can change the location of this file or turn off loading
// the plugins file with the 'pluginsFile' configuration option.
//
// You can read more here:
// https://on.cypress.io/plugins-guide
// ***********************************************************

// This function is called when a project is opened or re-opened (e.g. due to
// the project's config changing)
const data = require('../fixtures/test-data.json');
const gmail_tester = require('gmail-tester');
const path = require('path');
const tokenFileName = 'token_qa.json'; //token file is inside plugins/ directory
const credentialsFileName = 'credentials_qa.json'; //credentials is inside plugins/ directory
const clipboardy = require('clipboardy');
/**
 * @type {Cypress.PluginConfig}
 */
// eslint-disable-next-line no-unused-vars

module.exports = (on, config) => {
  on('task', {
    checkInbox: async args => {
      const { include_body, include_attachments, subject, after, to, from = data.outbox.email } = args;
      const email = await gmail_tester.check_inbox(path.resolve(__dirname, credentialsFileName), path.resolve(__dirname, tokenFileName), {
        from,
        wait_time_sec: 15,
        max_wait_time_sec: 300,
        include_body: true,
        include_attachments: true,
        after: new Date(Date.now() - 1000 * 60),
        to,
        subject,
      });
      return email; //this task returns one email (JSON object)
    },
  });

  on('task', {
    getClipboard() {
      return clipboardy.readSync();
    },
  });
};
