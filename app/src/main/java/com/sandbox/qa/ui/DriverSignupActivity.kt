package com.sandbox.qa.ui

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import com.sandbox.qa.R

/**
 * "Become a driver" signup form.
 *
 * A classic View-based (XML layout) screen inside an otherwise pure-Compose
 * app. Extends the framework [Activity] on purpose (no AppCompat, no extra
 * dependencies). Fully offline and deterministic: no network, no delays,
 * plain findViewById wiring.
 */
class DriverSignupActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_signup)

        findViewById<ImageButton>(R.id.driver_back_button).setOnClickListener { finish() }

        val nameInput = findViewById<EditText>(R.id.driver_name_input)
        val carInput = findViewById<EditText>(R.id.driver_car_input)
        val submitButton = findViewById<Button>(R.id.driver_submit_button)
        val errorText = findViewById<TextView>(R.id.driver_error_text)
        val successText = findViewById<TextView>(R.id.driver_success_text)

        submitButton.setOnClickListener {
            val name = nameInput.text.toString().trim()
            val car = carInput.text.toString().trim()
            if (name.isEmpty() || car.isEmpty()) {
                // Any empty field: show the error, hide any previous success.
                errorText.visibility = View.VISIBLE
                successText.visibility = View.GONE
            } else {
                // Both fields filled: show success, hide the error.
                errorText.visibility = View.GONE
                successText.visibility = View.VISIBLE
            }
        }
    }
}
