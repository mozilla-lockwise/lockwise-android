package mozilla.lockbox.presenter

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v7.app.AppCompatActivity
import android.util.Log
import mozilla.lockbox.R
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.action.RouteAction.*
import mozilla.lockbox.store.RouteStore
import mozilla.lockbox.view.FxALoginFragment
import mozilla.lockbox.view.WelcomeFragment


class IntentPresenter(private val activity: AppCompatActivity, routeStore: RouteStore = RouteStore.shared) {

    val welcome: WelcomeFragment by lazy { WelcomeFragment() }
    val login: FxALoginFragment by lazy { FxALoginFragment() }

    init {
        routeStore.routes.subscribe { a -> route(a) }
    }

    fun onViewReady() {
        replaceFragment(this.welcome, false)
    }

    private fun replaceFragment(frag: Fragment, backable: Boolean = true) {
        val tx = activity.supportFragmentManager.beginTransaction()
        tx.replace(R.id.root_content, frag)
        if (backable) {
            tx.addToBackStack(null)
        }
        tx.commit()
    }

    private fun clearBackStack() {
        val fm = activity.supportFragmentManager
        if (fm.backStackEntryCount > 0) {
            val base = fm.getBackStackEntryAt(0)
            fm.popBackStackImmediate(base.id, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        }
    }
    
    fun route(action: RouteAction) {
        when (action) {
            LOGIN -> {
                replaceFragment(login)
            }
            WELCOME -> {
                clearBackStack()
                replaceFragment(welcome, false)
            }
            ITEMLIST -> {
                clearBackStack()
            }
        }
    }
}