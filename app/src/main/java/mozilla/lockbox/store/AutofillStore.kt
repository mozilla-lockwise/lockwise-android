package mozilla.lockbox.store

import android.content.Context
import android.os.Build
import android.view.autofill.AutofillManager
import androidx.annotation.RequiresApi

open class AutofillStore : ContextStore {

    open lateinit var autofillManager: AutofillManager

    companion object {
        val shared = AutofillStore()
    }

    override fun injectContext(context: Context) {
        autofillManager = context.getSystemService(AutofillManager::class.java) as AutofillManager
    }

    open val isAutofillEnabledAndSupported: Boolean
        @RequiresApi(Build.VERSION_CODES.O)
        get() = autofillManager.hasEnabledAutofillServices() && autofillManager.isAutofillSupported
}