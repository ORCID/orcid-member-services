const fs = require('fs');
const helpers = require('./helpers.js');
const fileName = '../fixtures/test-data.json';
const file = require(fileName);

const newUser = helpers.newUser;
file.member.users.newUser.email = newUser.email;
file.testString = newUser.testString;

fs.writeFile(fileName, JSON.stringify(file, null, 2), function writeJSON(err) {
  if (err) return console.log(err);
});
