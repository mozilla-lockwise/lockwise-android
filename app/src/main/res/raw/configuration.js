
const FORM_ELEMENT_SELECTOR = "input, button, *[role=button]";
const FORM_ELEMENT_RECOGNIZERS = {
  passwordChange: [
    {
      password   : [ /password.+(old|current)/i ],
      newPassword: [ /password.+(new|confirm)/i ],
      submit     : [ /save|submit|change|update/i ],
    },
    {
      // a name or id specifically `password`
      password   : [ /[\[\s]password[\]\s]/i ], 
      newPassword: [ /password.+(new|confirm)/i ], 
      submit     : [ /save|submit|change|update/i ],
    },
    {
      password   : [ /password.+(old|current)/i ],
      // a name or id specifically `password`
      newPassword: [ /[\[\s]password[\]\s]/i, /password.+(new|confirm)/i ],
      submit     : [ /save|submit|change|update/i ],
    },
    {
      // this only has one password field in, but we should be 
      // wary of confusing it with a password challenge or login 
      // form.
      newPassword: [ /password.+user/i ],
      submit     : [ /submit|save|change|update/i ],
    },
  ],

  login: [
    {
      username: [ /username|email/i ],
      password: [ /password/i ],
      submit  : [ /login|submit|sign.?in/i ],
    },
    {
      password: [ /password/i ],
      submit  : [ /login|submit|sign.?in|next/i ],
    },
    {
      username: [ /username|email/i ],
      submit  : [ /login|submit|sign.?in|next/i ],
    },
  ]
};

const DESTINATION_RECOGNIZERS = {
  logout: {
    login        : [ /login/i, /sign.?in/i ],
  }
}

const DESTINATION_ERRORS = {
  login           : "NOT_FOUND_LOGIN",
  passwordChange  : "NOT_FOUND_PASSWORD_CHANGE",
  logout          : "NOT_FOUND_LOGOUT"
}

const DESTINATION_INFORMATION = {
  login: (map) => {
    const form = map.get("form");
    const formAction = form.action || form.location.toString();
    const hostname = document.location.origin;
    if (formAction) {
      try {
        const formActionOrigin = new URL(formAction).origin;
        return { formActionOrigin, hostname };
      } finally {
        // NOP
      }
    }
  },

  passwordChange: (map) => {
    const field = map.get("newPassword") || map.get("password");
    // We gather some attributes about characteristics of the password.
    // https://html.spec.whatwg.org/multipage/input.html#attr-input-pattern
    // Not handled:
    // https://developer.apple.com/documentation/security/password_autofill/customizing_password_autofill_rules
    const { 
      pattern,
      maxlength,
      minlength
    } = field;

    const obj = {};
    try {
      if (pattern) {
        obj.pattern = new RegExp(pattern).source;
      }
    } catch (e) {/* NOOP*/}

    try {
      obj.minLength = Integer.parseInt(minlength, 10);
    } catch (e) {/* NOOP*/}

    try {
      obj.maxLength = Integer.parseInt(maxlength, 10);
    } catch (e) {/* NOOP*/}

    return obj;
  },
}

const LINK_SELECTOR = "a, button, input[type=button], *[role=button]";
const LINK_RECOGNIZERS = {
  menu          : [ /nav(igation)?/i, /more/i ], 
  login         : [ /login/i, /sign.?in/i ],
  loginWithEmail: [],

  profile       : [ /profile/i, /\/$username/ ],
  settings      : [ /settings/i, /pref(erence)?s/i, /(profile|$username).*edit/, /edit.*(profile|$username)/ ],
  account       : [ /account/i ],
  security      : [ /security/i ],
  changePassword: [ /password/i, /reset/i, /change/i ],

  logout        : [ /logout/i, /sign.?out/ ],
};

const LINK_PATHS = {
  login           : ["menu", "login", "loginWithEmail"],
  passwordChange  : ["menu", "profile", "account", "settings", "security", "changePassword"],
  logout          : ["menu", "profile", "account", "settings", "logout"],
};


const ERROR_MESSAGE_SELECTOR = "*[class*=error i]";

const NUISANCE_FACTORS = {
  terms_of_service: {
    selector   : FORM_ELEMENT_SELECTOR,
    recognizers : {
      checkbox  : [ /checkbox.+tos/i, /checkbox.+terms/i ]
    },
    code: "BLOCKED_BY_TOS"
  },

  captcha: {
    selector   : "iframe",
    recognizers : {
      checkbox  : [ /captcha/i, /recaptcha/i ]
    },
    code: "BLOCKED_BY_CAPTCHA"
  },

  multifactor: {
    name        : 'multifactor',
    selectors   : FORM_ELEMENT_SELECTOR,
    recognizers : {
      checkbox  : [ /passcode/i ]
    },
    code: "BLOCKED_BY_2FA"
  },
};

Object.assign(exports, {
  FORM_ELEMENT_RECOGNIZERS,
  LINK_RECOGNIZERS,
});