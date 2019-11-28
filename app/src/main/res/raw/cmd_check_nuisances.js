const checkNuisanceFactors = (root, factors) => {
  const found = Object.keys(factors)
    .flatMap(name => {
      const { selector, recognizers, code } = factors[name];

      const results = findTypedElements(root, selector, recognizers);

      const visibleElements = [...results.values()]
        .flatMap(_ => _)
        .filter(isVisible);

      return visibleElements.length > 0 ? code : null;
    })
    .filter(_ => _);

  const firstFound = found.shift();
  if (firstFound) {
    backChannel.onFail("nuisance", firstFound);
    return true;
  }
};