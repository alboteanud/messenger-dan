package com.craiovadata.android.messenger.util

import com.craiovadata.android.messenger.MessagesActivity
import com.craiovadata.android.messenger.MessagesActivity.Companion.MESSAGES
import com.craiovadata.android.messenger.model.Message
import com.craiovadata.android.messenger.model.User
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

object DbUtil {
    fun unsubscribeAllTopics(firestore: FirebaseFirestore) {
        val uid = FirebaseAuth.getInstance().uid
        firestore.collection("users/${uid}/rooms").get().addOnSuccessListener { snapshot ->
            for (document in snapshot) {
                FirebaseMessaging.getInstance().unsubscribeFromTopic(document.id)
            }

        }
    }

    fun subscribeToAllTopic(firestore: FirebaseFirestore) {
        val uid = FirebaseAuth.getInstance().uid
        firestore.collection("users/${uid}/rooms").get().addOnSuccessListener { snapshot ->
            for (document in snapshot) {
                FirebaseMessaging.getInstance().subscribeToTopic(document.id)
            }

        }
    }

    fun writeNewUser(firestore: FirebaseFirestore, firebaseUser: FirebaseUser) {

        val email = firebaseUser.email.toString()
        val displayName = firebaseUser.displayName.toString()

        val keywords = Util.getKeywords(email, displayName)

        val user = User(email, displayName, firebaseUser.photoUrl.toString(), keywords)
        val uid = firebaseUser.uid

        firestore.collection("users").document(uid).set(user)
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
        return firestore.collection("${MessagesActivity.USERS}/${uid}/rooms")
    }

}