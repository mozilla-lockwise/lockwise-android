const examine = (destination) => {
  const fn = DESTINATION_INFORMATION[destination];
  const map = findDestination(destination);

  if (typeof fn !== 'function' || !map) {
    backChannel.onExamination(destination, {});
    return;
  }

  const obj = fn(map)
  backChannel.onExamination(destination, obj);
};