package com.craiovadata.android.messenger.model

import android.text.TextUtils
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Model POJO for a message.
 */
data class Message(
        var userId: String? = null,
        var displayName: String? = null,
        var photoUrl: String? = null,
        var text: String? = null,
        @ServerTimestamp var timestamp: Date? = null
) {

    constructor(user: FirebaseUser, text: String) : this() {
        this.userId = user.uid
        this.displayName = user.displayName
        this.photoUrl = user.photoUrl.toString()
        if (TextUtils.isEmpty(this.displayName)) {
            this.displayName = user.email
        }

        this.text = text
    }
}
