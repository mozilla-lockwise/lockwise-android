/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.lockbox.view

import android.os.Bundle
import android.os.PersistableBundle
import android.support.v7.app.AppCompatActivity
import mozilla.lockbox.R

class RootActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.root_activity)

        val tx = supportFragmentManager.beginTransaction()
        val frag = WelcomeFragment()
        tx.add(R.id.root_content, frag)
        tx.commit()
    }
}