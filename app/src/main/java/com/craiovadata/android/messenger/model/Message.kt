package com.craiovadata.android.messenger.model

import android.text.TextUtils
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.ServerTimestamp

data class Message(
        var uid: String?,
        var author: String?,
        var text: String?,
        var photoUrl: String?,
        @ServerTimestamp var timestamp: Timestamp?
) {

    constructor() : this(null, null, null, null, null)

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