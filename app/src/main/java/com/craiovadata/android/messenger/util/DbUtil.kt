package com.craiovadata.android.messenger.util

import android.content.Context
import android.content.Context.MODE_PRIVATE
import com.craiovadata.android.messenger.model.Message
import com.craiovadata.android.messenger.model.SearchedUser
import com.craiovadata.android.messenger.model.User
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.iid.FirebaseInstanceId


object DbUtil {

    fun removeRegistration(context: Context, uid: String?) {
        val firestore = FirebaseFirestore.getInstance()
        val tokensRef = firestore.collection("users/${uid}/tokens")
        val token = getRegistrationToken(context)
        if (token != null) {
            tokensRef.document(token).delete()
        }
    }

    fun getRegistrationToken(context: Context): String? {
        return context.getSharedPreferences("_", MODE_PRIVATE).getString("token", FirebaseInstanceId.getInstance().token)
    }

    fun writeNewUser(context: Context, firebaseUser: FirebaseUser) {
        val db = FirebaseFirestore.getInstance()
        val email = firebaseUser.email.toString()
        val displayName = firebaseUser.displayName.toString()
        val photoUrl = firebaseUser.photoUrl.toString()

        val user = User(email, displayName, photoUrl)
        val uid = firebaseUser.uid
        val batch = db.batch()
        val usrRef = db.collection("users").document(uid)
        batch.set(usrRef, user)

        // TODO build keywords in the cloud (functions)
        val keywords = Util.getKeywords(email, displayName)
        val keywordsRef = db.document("userKeywords/${uid}")
        val searchUser = SearchedUser(displayName, photoUrl, keywords)
        batch.set(keywordsRef, searchUser)

        val registrationToken = getRegistrationToken(context)
        if (registrationToken != null) {
            val ref = db.document("userTokens/${uid}")
            val regTokenObj = HashMap<String, Any>()
            batch.set(ref, regTokenObj, SetOptions.merge())
        }

        batch.commit()
    }

    fun addMessage(roomID: String, message: Message, palId: String?, uid: String): Task<Void> {

        val msgRef = getRoomsRef(uid).document(roomID).collection(MESSAGES).document()
        val batch = FirebaseFirestore.getInstance().batch()

        batch.set(msgRef, message)
        batch.set(getRoomsRef(palId!!).document(roomID).collection(MESSAGES).document(msgRef.id), message)

        val usr = HashMap<String, Any>()
        usr.put("lastMsgAuthor", message.displayName!!)
        usr.put("lastMsg", message.text!!)

        batch.update(getRoomsRef(uid).document(roomID), usr)
        batch.update(getRoomsRef(palId).document(roomID), usr)

        return batch.commit()
    }

    fun getRoomsRef(uid: String): CollectionReference {
        val firestore = FirebaseFirestore.getInstance()
        return firestore.collection("${USERS}/${uid}/rooms")
    }

}