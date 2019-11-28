const _checkNoErrors = (destination) => {
  const errorMessages = [ ...document.querySelectorAll(ERROR_MESSAGE_SELECTOR) ]
    .filter($e => $e.tagName !== 'INPUT')
    .filter(isVisible);

  console.log("errors", errorMessages);

  return errorMessages.length === 0;
};

const _checkMultipageForm = (destination, successIfPageChanged) => {
  // If we have a non-errored submit, and the process is finished
  // when we we've moved away from the destination, then perhaps
  // we haven't finished yet. (e.g. login process spread across
  // multiple pages)
  return successIfPageChanged && findDestination(destination);
};

const confirmSuccess = (destination, successIfPageChanged, ...args) => {
  waitForSettling(100, document, 300)
    .then(() => {
      if (!_checkNoErrors(destination)) {
        return backChannel.onFail(destination, "BAD_CREDENTIALS");
      } else if (_checkMultipageForm(destination, successIfPageChanged)) {
        return backChannel.onArrival(destination);
      } else {
        return backChannel.onFormFillSuccess(destination);
      }
    })
    .catch(e => {
      console.error(e);
      backChannel.onFail(destination, "UNKNOWN_ERROR")
    });
};