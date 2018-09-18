/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.lockbox.view
import android.os.Bundle
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_main.*
import mozilla.lockbox.R
import mozilla.lockbox.action.RouteAction
import mozilla.lockbox.flux.Dispatcher

class MainActivity : AppCompatActivity() {
    private lateinit var drawer: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        this.drawer = appDrawer

        // setup drawer interactions
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_menu)
            title = getString(R.string.app_name)
        }

        navView.setNavigationItemSelectedListener { menuItem ->
            drawer.closeDrawers()

            // TODO: wire up actions to navigation selections

            true
        }

        helloWorld.setOnClickListener {
            Dispatcher.shared.dispatch(RouteAction.WELCOME)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            android.R.id.home -> {
                drawer.openDrawer(GravityCompat.START)
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
