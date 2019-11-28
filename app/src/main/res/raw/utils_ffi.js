const secureToken = "$secureToken";
// wrap the lockwise_ffi object (a native object) to use a secure token
// that is generated at insertion time.
const backChannel = {
  onTapBegin(action, ...args) {
    lockwise_ffi.onTapBegin(secureToken, action);
  },

  onTapEnd(action, ...args) {
    lockwise_ffi.onTapEnd(secureToken, action);
  },

  onFormFillSuccess(formName, ...args) {
    lockwise_ffi.onFormFillSuccess(secureToken, formName);
  },

  onArrival(destination, ...args) {
    lockwise_ffi.onArrival(secureToken, destination);
  },

  onExamination(destination, options, ...args) {
    lockwise_ffi.onExamination(secureToken, destination, JSON.stringify(options));
  },

  onFail(action, reason, ...args) {
    lockwise_ffi.onFail(secureToken, action, reason);
  }
};
