package com.craiovadata.android.messenger.util

import android.content.Context
import android.content.Context.MODE_PRIVATE
import com.craiovadata.android.messenger.model.Conversation
import com.craiovadata.android.messenger.model.Message
import com.craiovadata.android.messenger.model.SearchedUser
import com.craiovadata.android.messenger.model.User
import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp.now
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.messaging.FirebaseMessagingService
import java.util.*


object DbUtil {

    fun removeRegistration(context: Context, uid: String?) {
        val token = getRegistrationToken(context)
        if (token != null) {
            val tokensRef = FirebaseFirestore.getInstance().collection("$USERS/${uid}/$TOKENS")
            tokensRef.document(token).delete()
        }
        context.getSharedPreferences("_", FirebaseMessagingService.MODE_PRIVATE).edit().remove("token").apply()
    }

    fun getRegistrationToken(context: Context): String? {
        return context.getSharedPreferences("_", MODE_PRIVATE).getString(TOKEN, FirebaseInstanceId.getInstance().token)
    }

    fun writeNewUser(context: Context, firebaseUser: FirebaseUser?) {
        if (firebaseUser==null) return

        val db = FirebaseFirestore.getInstance()
        val email = firebaseUser.email.toString()
        val displayName = firebaseUser.displayName.toString()
        val photoUrl = firebaseUser.photoUrl.toString()

        val userM = User(email, displayName, photoUrl)
        val uid = firebaseUser.uid
        val batch = db.batch()
        val usrRef = db.collection(USERS).document(uid)
        batch.set(usrRef, userM)

        // TODO build keywords with cloud functions
        val keywords = Util.getKeywords(email, displayName)
        val keywordsRef = db.document("$USER_KEYWORDS/$uid")
        val searchUser = SearchedUser(uid, displayName, photoUrl, keywords)
        batch.set(keywordsRef, searchUser)

        val registrationToken = getRegistrationToken(context)
        if (registrationToken != null) {
            val ref = db.document("$USERS/$uid/$TOKENS/$registrationToken")
            val regTokenObj = HashMap<String, Any>()
            regTokenObj["updated"] = now()
            batch.set(ref, regTokenObj, SetOptions.merge())
        }

        batch.commit()
    }



}