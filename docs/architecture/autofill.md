## Autofill Framework Interaction

Currently, Lockwise for Android only supports the [Autofill Framework](https://developer.android.com/guide/topics/text/autofill.html) as provided by Google. Support for this framework is not universal among apps in the Play store, so autofill will not always behave as expected. Enumerated below are the simple strategies we use and their priorities in determining which views should receive autofill suggestions with Username and Password credentials.

## Heuristics for determining autofill-able views in native applications

1. Using the `autofillHints` attribute on `EditText` views
2. Using the `text` and `hint` attributes on `EditText` views
3. Assuming that a `TextView` with the relevant `autofillHints`, `text`, or `hint` attributes _immediately followed_ by an `EditText` view, should autofill the `EditText`
