package com.craiovadata.android.messenger.util

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.inputmethod.InputMethodManager
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.messaging.FirebaseMessagingService


object Util {

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

    fun removeRegistration(context: Context, uid: String?) {
        val token = getRegistrationToken(context)
        if (token != null) {
            val tokensRef = FirebaseFirestore.getInstance().collection("$USERS/${uid}/$TOKENS")
            tokensRef.document(token).delete()
        }
        context.getSharedPreferences("_", FirebaseMessagingService.MODE_PRIVATE).edit().remove("token").apply()
    }

    fun getRegistrationToken(context: Context): String? {
        return context.getSharedPreferences("_", Context.MODE_PRIVATE).getString(TOKEN, FirebaseInstanceId.getInstance().token)
    }

}
