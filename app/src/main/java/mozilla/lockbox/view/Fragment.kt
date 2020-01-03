/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.lockbox.view

import android.content.Context
import android.os.Bundle
import android.view.Gravity
import androidx.fragment.app.Fragment as AndroidFragment
import androidx.core.view.ViewCompat
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import kotlinx.android.synthetic.main.include_backable_filter.view.*
import mozilla.lockbox.R
import mozilla.lockbox.flux.Presenter
import mozilla.lockbox.support.assertOnUiThread

open class Fragment : AndroidFragment() {
    lateinit var presenter: Presenter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        presenter.onViewReady()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        presenter.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        presenter.onResume()
    }

    override fun onPause() {
        super.onPause()
        presenter.onPause()
    }

    fun closeKeyboard(view: View?) {
        val inputMethodManager = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    fun openKeyboard(view: View?) {
        val inputMethodManager = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    override fun onCreateAnimation(transit: Int, enter: Boolean, nextAnim: Int): Animation? {
        if (nextAnim == R.anim.slide_in_bottom) {
            val nextAnimation = AnimationUtils.loadAnimation(context, nextAnim)
            nextAnimation.setAnimationListener(object : Animation.AnimationListener {
                private var startZ = 0f
                override fun onAnimationStart(animation: Animation) {
                    view?.apply {
                        startZ = ViewCompat.getTranslationZ(this)
                        ViewCompat.setTranslationZ(this, 1f)
                    }
                }

                override fun onAnimationEnd(animation: Animation) {
                    // Short delay required to prevent flicker since other Fragment wasn't removed just yet.
                    view?.apply {
                        this.postDelayed({ ViewCompat.setTranslationZ(this, startZ) }, 100)
                    }
                }

                override fun onAnimationRepeat(animation: Animation) {}
            })
            return nextAnimation
        } else {
            return null
        }
    }

    /**
     * Called if the back button is pressed.
     *
     * @return `true` if the back button event has been handled. By default, returns false, in which
     * case Android handles the event.
     */
    fun onBackPressed(): Boolean = presenter.onBackPressed()
}
