const { test } = require("tap");
const path = require("path");
const rawPath = path.join(__dirname, "../../../src/main/res/raw");
console.log(`rawPath`, rawPath);
const { FORM_ELEMENT_RECOGNIZERS } = require(path.join(rawPath, "configuration"));
const { findItemBucketsByRecognizers } = require(path.join(rawPath, "utils_recognizers"));

const passwordChange = {
  "account.bbc.com":
    [
      ['none', 'button|primary-nav__toggle link'],
      ['none', 'button|primary-nav__page-overlay'],
      ['password', 'password|current-password-input|currentPassword|field__input field__input--password-toggle|true|true|none|off||'],
      ['newPassword', 'password|new-password-input|newPassword|field__input field__input--password-toggle|true|true|none|off||'],
      ['none', 'hidden|email|email|nobbcno@mailinator.com'],
      ['submit', 'submit|button'],
      ['none', 'hidden|_csrf|Z1U192S1-DezzusCWAjK-1VpAI-qplzT66E0'],
    ]
};

const login = {
  "account.bbc.com":
    [
      ['none', "hidden|jsEnabled|true"],
      ['username', "email|user-identifier-input|username|field__input|true|true|none|username|off|||nobbcno@mailinator.com"],
      ['password', "password|password-input|password|field__input field__input--password-toggle|true|true|none|current-password|off||"],
      ['submit', "submit|submit-button|button button--full-width|Sign in"],
      ['none', "hidden|attempts|0"],
    ]
};

test('Paths for testing works', t => {
  t.ok(FORM_ELEMENT_RECOGNIZERS);
  t.equal('function', typeof findItemBucketsByRecognizers);
  t.end();
});

const testRecognizer = (t, recognizerType, siteName, array, recognizer) => {
  const tabbedArray = array.map(pair => [pair[0], pair[1].replace(/\|/g, "\t")]);
  const maps = findItemBucketsByRecognizers(tabbedArray, recognizer, true);
  t.ok(maps.length, `${maps.length} ${recognizerType} results found for ${siteName}`);

  const correctlyClassified = maps.reduce((acc, map) => {
    let ok = true;
    for (const [elementType, elements] of map.entries()) {
      for (const element of elements) {
        if (element !== elementType) {
          ok = false;
          console.log(`${recognizerType} recognizer ${map.recognizerIndex} failed on ${siteName}:`, elementType, '=>', elements);
          break;
        }
      }
    }
    return acc && ok;
  }, true);

  if (!correctlyClassified) {
    console.log(maps);
  }
  t.ok(correctlyClassified,`all ${recognizerType} recognizers on ${siteName}`);
  
};

const testRecognizers = (t, recognizerType, recognizers) => {
  const recognizer = FORM_ELEMENT_RECOGNIZERS[recognizerType];
  for (const [siteName, inputArray] of Object.entries(recognizers)) {
    testRecognizer(t, recognizerType, siteName, inputArray, recognizer);
  }
};

test('Login form recognizers work', t => {
  testRecognizers(t, "login", login);
  t.end();
});

test('PasswordChange form recognizers work', t => {
  testRecognizers(t, "passwordChange", passwordChange);
  t.end();
});