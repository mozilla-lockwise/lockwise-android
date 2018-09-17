package com.firefox.lockbox.firefoxlockbox

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.TextView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val textView = this.findViewById<TextView>(R.id.hello_world)
        textView.setText(R.string.hello_world)
        this.supportActionBar?.title = getString(R.string.app_name)
    }
}
