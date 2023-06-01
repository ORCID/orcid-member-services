let newUser = () => {
  let date = new Date().toISOString().substring(0, 19).replace(/:/g, '-').replace(/\./g, '-').toLowerCase();
  let testString = '  !@#$%^&*()-=_  ' + randomString() + ' ' + date + '  ';
  return {
    email: 'qa' + '+mp_cy_' + date + '@orcid.org',
    testString,
  };
};

let randomString = () => {
  let text = '';
  let possible = 'ñáéíóú-北查爾斯頓工廠的安全漏洞已經引起了航空公司和監管機構的密切關注';

  for (var i = 0; i < 2; i++) text += possible.charAt(Math.floor(Math.random() * possible.length));

  return text;
};

module.exports.newUser = newUser();
