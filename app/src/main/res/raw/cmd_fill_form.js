const fillForm = (formName, inputValues) => {
  const formObj = findForm(document, FORM_ELEMENT_RECOGNIZERS[formName]);
  const submit = formObj.get("submit").filter(isButton).shift();
  return fillFormFields(formObj, inputValues)
    .then(ret => tapButton(submit, formName))
    .catch(e => console.log("error", e));
};

const showForm = (formName) => {
  const formObj = findForm(document, FORM_ELEMENT_RECOGNIZERS[formName]);
  console.log(`Form ${formName}`, formObj);
};

const logElements = (elementType) => {
  const selector = elementType === "a" ? LINK_SELECTOR : FORM_ELEMENT_SELECTOR;
  const array = 
    [...document.querySelectorAll(LINK_SELECTOR)]
    .map($e => ['_', _generateString($e, "|")]);

  console.log(array);
  if (typeof copy === 'function') {
    copy(array);
  }
};