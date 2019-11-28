const findForm = (root, formRecognizer) => {
  const recognizers = Array.isArray(formRecognizer) ? formRecognizer : [formRecognizer];
  const forms = [...root.querySelectorAll("form"), root]
    .flatMap(form => 
      recognizers.flatMap(recognizer => {
        const numRecognizers = Object.keys(recognizer).length;
        const obj = findTypedElements(form, FORM_ELEMENT_SELECTOR, recognizer);
        return (obj.size === numRecognizers) ? obj.set("form", form) : null;
      })  
    )
    .filter(_ => _)
    .sort((a, b) => _numInputs(b) - _numInputs(a))

  return forms.shift();
};

const _numInputs = (formObj) => {
  return [...formObj.values()]
    .filter(Array.isArray)
    .flatMap(_ => _)
    .filter(b => !isButton(b))
    .length;
};

const fillFormFields = (formObj, inputValues) => {
  // formObj is a map
  // inputName => [elements]

  // inputValues is an object,
  // inputName => string value

  const initial = Promise.resolve();
  const fillFields = Object.keys(inputValues).reduce((promise, key) => {
    const elements = formObj.get(key);
    const value = inputValues[key];

    if (!elements) {
      return promise.then(() => Promise.resolve());
    }

    return promise.then(
      ret => Promise.allSettled(
        elements.map(
          $el => fillText($el, key, value)
        )
      )
    );
  }, initial);

  return fillFields;
};

const findDestination = (destination) => {
  const formRecognizer = FORM_ELEMENT_RECOGNIZERS[destination];
  if (formRecognizer) {
    return findForm(document, formRecognizer);
  }

  const destinationRecognizer = DESTINATION_RECOGNIZERS[destination];
  if (destinationRecognizer) {
    return findTypedElements(document, LINK_SELECTOR, destinationRecognizer);
  }
}
