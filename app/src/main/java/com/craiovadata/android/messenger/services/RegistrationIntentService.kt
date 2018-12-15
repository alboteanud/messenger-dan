package com.craiovadata.android.messenger.services

import android.app.IntentService
import android.content.Intent

class RegistrationIntentService : IntentService("RegIntentService") {

    private val TAG = "RegIntentService"

    override fun onHandleIntent(intent: Intent?) {


    }

    private fun sendRegistrationToServer(token: String) {
        // Add custom implementation, as needed.
    }

}