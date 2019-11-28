const debounce = (fn, timeout) => {
  let timer;

  return (...args) => {
    if (timer) {
      clearTimeout(timer);
    }
    timer = setTimeout(() => fn(...args));
  };
};

const waitForSettling = (timeout, $watch = document, noMutationTimeout = 100 * timeout) => {
  return new Promise((resolve, reject) => {
      const config = {
        attributes: true, 
        childList: true, 
        subtree: true
      };

      const callback = () => {
        resolve();
        observer.disconnect();
      };
      const observer = new MutationObserver(debounce(callback, timeout));
      observer.observe($watch, config);

      if (noMutationTimeout) {
        setTimeout(callback, noMutationTimeout);
      }
  });
};

const timeoutPromise = (timeout) => new Promise(
  (resolve, reject) => setTimeout(resolve, timeout)
);

const actionPromise = (elementName, action, delay = 300) => 
  Promise.resolve()
    .then(() => backChannel.onTapBegin(elementName))
    .then(() => timeoutPromise(100))
    .then(action)
    .then(() => waitForSettling(delay))
    .then(() => backChannel.onTapEnd(elementName));

const tapButton = ($el, elementName) => actionPromise(
  elementName, 
  () => {
    if (typeof $el.click === "function") {
      $el.disabled = false;
      $el.click();
    } else if (typeof $el.submit === "function") {
      $el.submit();
    }
  }
);

const tap = tapButton;

const fillText = ($el, elementName, text) => 
  Promise.resolve()
    .then(() => {
      $el.dispatchEvent(new FocusEvent("focus", { bubbles: false }));
    })
    .then(() => timeoutPromise(10))
    .then(() => {
      const setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, "value").set;
      setter.call($el, text);
    })
    .then(() => timeoutPromise(10))
    .then(() => {
      $el.dispatchEvent(new Event('input', { bubbles: true }));
      $el.dispatchEvent(new Event('change', { bubbles: true }));
    })
    .then(() => timeoutPromise(100));