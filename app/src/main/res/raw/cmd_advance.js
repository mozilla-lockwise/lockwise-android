const checkArrival = (destination) => {
  const map = findDestination(destination);
  if (map && map.size > 0) {
    backChannel.onArrival(destination)
    return true;
  }
};

const advance = (destination, ...args) => {
    // if already at destination
  // i.e. if we can find the form for the destination
  if (checkArrival(destination)) {
    return;
  }

  const links = findTypedElements(document, LINK_SELECTOR, LINK_RECOGNIZERS);

  // find the paths available
  const linkPath = LINK_PATHS[destination] || [];

  const nextLink = linkPath
    .map(linkName => new Array(linkName, links.get(linkName)))
    .filter(a => a[1])
    .map(a => new Array(a[0], a[1][0]))
    .pop();

  if (nextLink) {
    const [linkName, $el] = nextLink;
    console.log(`Tapping ${linkName}`);
    tapButton($el, linkName);
  } else {
    console.log(`Cannot find next step in path to ${destination}`);
    const notFound = DESTINATION_ERRORS[destination] || "NOT_FOUND_PASSWORD_CHANGE";
    backChannel.onFail(destination, notFound);
  }
};