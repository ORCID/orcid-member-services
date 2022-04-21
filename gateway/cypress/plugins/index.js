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
const gmail_tester = require('gmail-tester')
const path = require('path')
const tokenFileName = 'token_qa.json' //token file is inside plugins/ directory
const credentialsFileName = 'credentials_qa.json' //credentials is inside plugins/ directory
const clipboardy = require('clipboardy')
/**
 * @type {Cypress.PluginConfig}
 */
// eslint-disable-next-line no-unused-vars

module.exports = (on, config) => {
  on('task', {
    checkInbox: async (args) => { 
      const { from, to, subject } = args.options
      const email = await gmail_tester.check_inbox(
        path.resolve(__dirname, credentialsFileName),
        path.resolve(__dirname, tokenFileName),
        args.options
        )
        return email //this task returns one email (JSON object)
      },    
    });

  on('task', {
    getClipboard () {
      return clipboardy.readSync();
    }
  });
}
