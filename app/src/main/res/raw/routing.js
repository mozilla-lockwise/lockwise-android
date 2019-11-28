if (checkNuisanceFactors(document, NUISANCE_FACTORS)) {
  return "nuisance_factor_found";
}

switch (action) {
  case "checkArrival":
    checkArrival(...args);
    break;

  case "advance":
    advance(...args);
    break;

  case "confirmSuccess":
    confirmSuccess(...args);
    break;

  case "fillForm":
    fillForm(...args);
    break;

  case "examine":
    examine(...args);
    break;

  case "showForm":
    showForm(...args);
    break;

  case "logElements":
    logElements(...args);
    break;

  default:
    console.error("Unknown action: ${action}");
}

return "routing_called";