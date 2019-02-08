package com.craiovadata.android.messenger.util

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.inputmethod.InputMethodManager
import com.craiovadata.android.messenger.MainActivity
import com.craiovadata.android.messenger.model.User
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FieldValue.serverTimestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firestore.v1beta1.DocumentTransform
import java.util.HashMap


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

    fun removeRegistration() {

        FirebaseAuth.getInstance().currentUser?.let { user ->

            FirebaseInstanceId.getInstance().instanceId
                    .addOnCompleteListener(OnCompleteListener { task ->

                        if (!task.isSuccessful) {
                            Log.w(MainActivity.TAG, "getInstanceId failed", task.exception)
                            return@OnCompleteListener
                        }

                        val token = task.result?.token
                        token?.let { token ->
                            FirebaseFirestore.getInstance().collection("$USERS/${user.uid}/$TOKENS")
                                    .document(token).delete()
                        }
                    })
        }


    }


    fun sendRegistrationToServer(token: String?) {

        FirebaseAuth.getInstance().currentUser?.let { user ->
            val ref = FirebaseFirestore.getInstance().document(
                    "$USERS/${user.uid}/$TOKENS/$token")

            val hashMap = HashMap<String, Any>()
            hashMap["updated"] = serverTimestamp()
            hashMap["uid"] = user.uid
            ref.set(hashMap)
        }
    }

    fun getRegistrationAndSendToServer() {

        FirebaseInstanceId.getInstance().instanceId
                .addOnCompleteListener(OnCompleteListener { task ->

                    if (!task.isSuccessful) {
                        Log.w(MainActivity.TAG, "getInstanceId failed", task.exception)
                        return@OnCompleteListener
                    }

                    val token = task.result?.token
                    sendRegistrationToServer(token)
                })
    }

}
