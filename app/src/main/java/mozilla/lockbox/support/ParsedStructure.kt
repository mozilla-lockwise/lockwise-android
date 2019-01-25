package mozilla.lockbox.support

import android.view.autofill.AutofillId

data class ParsedStructure(
    var usernameId: AutofillId? = null,
    var passwordId: AutofillId? = null,
    var webDomain: String? = null
)


