package com.craiovadata.android.messenger.util

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.inputmethod.InputMethodManager
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability


object Util {

    fun getKeywords(email: String, displayName: String): MutableList<String> {
        val emailLC = email.toLowerCase()
        val displayNameLC = displayName.toLowerCase()

        val keywords: ArrayList<String> = arrayListOf()

        val username = usernameFromEmail(emailLC)               // alboteanud
        keywords.add(username)                                  // add directly to list (derivatives are added from email)

        addWordDerivToList(emailLC, keywords)                   // alboteanud@gmail.com
        addWordDerivToList(displayNameLC, keywords)

        val displayNameS = wordsFromString(displayNameLC)       // alb, albo, albot, albote
        for (word in displayNameS) addWordDerivToList(word, keywords)

        return keywords
    }

    // add word and derivatives to list
    private fun addWordDerivToList(word: String, keywords: ArrayList<String>) {
        if (word.length < 4) return

        if (!keywords.contains(word))
            keywords.add(word)

        val endLettersIndex =
                if (word.length > 7) 7
                else word.length - 1
        for (i in 4..endLettersIndex) {
            val substr = word.substring(0, i)
            if (!keywords.contains(substr))
                keywords.add(substr)
        }

    }

    private fun usernameFromEmail(email: String): String {
        return if (email.contains("@")) {
            email.split("@".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
        } else {
            email
        }
    }

    private fun wordsFromString(s: String): Array<String> {
        return if (s.contains(" ")) {
            s.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        } else {
            arrayOf(s)
        }
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    fun checkPlayServices(activity: Activity): Boolean {

        val apiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = apiAvailability.isGooglePlayServicesAvailable(activity)
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(activity, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST).show()
            } else {
                Log.i("Util", "This device is not supported.")
//                activity.finish()
            }
            return false
        }
        return true
    }

    private const val PLAY_SERVICES_RESOLUTION_REQUEST = 9000

    private fun hideKeyboard(activity: Activity) {
        val view = activity.currentFocus
        if (view != null) {
            (activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                    .hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

}
