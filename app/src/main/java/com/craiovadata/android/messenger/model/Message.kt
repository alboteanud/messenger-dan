package com.craiovadata.android.messenger.model

import android.text.TextUtils
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.ServerTimestamp


//@IgnoreExtraProperties
data class Message(
        var uid: String? = null,
        var author: String? = null,
        var text: String? = null,
        var photoUrl: String? = null,
        @ServerTimestamp var timestamp: Timestamp? = null
) {

    constructor(user: FirebaseUser, msgText: String) : this() {
        this.uid = user.uid
        this.author = user.displayName
        if (TextUtils.isEmpty(this.author)) {
            this.author = user.email
        }

        this.text = msgText
        this.photoUrl = user.photoUrl.toString()
    }


}