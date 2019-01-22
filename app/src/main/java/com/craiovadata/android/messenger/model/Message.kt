package com.craiovadata.android.messenger.model

import android.text.TextUtils
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.ServerTimestamp
import java.util.*

data class Message(
        var uid: String? = null,
        var author: String? = null,
        var photoUrl: String? = null,
        var text: String? = null,
        @ServerTimestamp var timestamp: Date? = null
) {

    constructor(user: FirebaseUser, text: String) : this() {
        uid = user.uid
        author = user.displayName
        photoUrl = user.photoUrl.toString()
        if (TextUtils.isEmpty(author)) author = user.email
        this.text = text
    }
}
