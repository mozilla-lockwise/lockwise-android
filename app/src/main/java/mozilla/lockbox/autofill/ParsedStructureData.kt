package mozilla.lockbox.autofill

data class ParsedStructureData<AutofillId>(
    var usernameId: AutofillId? = null,
    var passwordId: AutofillId? = null,
    var webDomain: String? = null,
    var packageName: String
)