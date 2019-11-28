const _generateString = (element, sep = "\t") => {
  const initialKeys = ["type", "id", "name", "class"];
  const initalValues = initialKeys
    .map(k => element.getAttribute(k))
    .filter(_ => _);

  const excludedKeys = new Set(["autocomplete"]);

  const sortedAttributes =
    [...element.attributes]
    .filter($a => initialKeys.indexOf($a.name) < 0)
    .filter($a => !excludedKeys.has($a.name))
    .sort(
      (a, b) => a.name > b.name ? 1 : -1
    )
    .map(
      a => a.value
    );

  return [...initalValues, ...sortedAttributes].join(sep);
};

const _bucketELement = (element, elementString, recognizer, obj) => {
  for (let [elementType, search] of Object.entries(recognizer)) {
    const matched = search.reduce((acc, pattern) => {
      const stringMatched = (typeof pattern === "string") && (elementString.indexOf(pattern) >= 0);
      const reMatched = (pattern instanceof RegExp) && elementString.match(pattern) !== null;

      return acc || stringMatched || reMatched;
    }, false);

    if (matched) {
      const elements = obj.get(elementType);
      if (elements) {
        elements.push(element);
      } else {
        obj.set(elementType, [ element ]);
      }
    }
  }
};

const _bucketElements = (array, recognizer, obj) => {
  for (const [element, elementString] of array) {
    _bucketELement(element, elementString, recognizer, obj);
  }
  return obj;
};

const findItemBucketsByRecognizers = (array, recognizers, completeMatch = false) => {
  recognizers = Array.isArray(recognizers) ? recognizers : [recognizers];
  let index = 0;
  return recognizers.map(recognizer => {
      const obj = _bucketElements(array, recognizer, new Map());
      obj.recognizerIndex = index ++;
      return (!completeMatch || obj.size >= Object.keys(recognizer).length) ? obj : null;
    })
    .filter(_ => _);
};

const findElementBucketsByRecognizers = (root, selector, ...args) => {
  const array = [...root.querySelectorAll(selector)]
    .map($el => [$el, _generateString($el)]);
  const objs = findItemBucketsByRecognizers(array, ...args);
  objs.forEach(obj => {
    obj.rootElement = root;
  });
  return objs;
};

// This function takes a document/root, generates a map of attribute values to elements that match the 
// given selector, and attribute matches map.
const findTypedElements = (root, selector, recognizer) => {
  return findElementBucketsByRecognizers(root, selector, recognizer).shift();
};

const isVisible = ($el) => {
  const {clientWidth, clientHeight} = $el;
  return clientWidth > 0 && clientHeight > 0;
};

const isButton = ($el) => {
  const { type, tagName } = $el;
  const role = $el.getAttribute("role");
  return type === "submit" || role === "button"|| type === "button" || tagName === "BUTTON";
};

Object.assign(exports, {
  findElementBucketsByRecognizers,
  findItemBucketsByRecognizers,
});