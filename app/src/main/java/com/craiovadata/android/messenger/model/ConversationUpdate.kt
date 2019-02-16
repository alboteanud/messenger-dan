package com.craiovadata.android.messenger.model

import android.text.TextUtils
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.ServerTimestamp


//@IgnoreExtraProperties
data class ConversationUpdate(
        var author: String? = null,
        var text: String? = null,
        @ServerTimestamp var timestamp: Timestamp? = null
) {

    constructor(user: FirebaseUser, text: String) : this() {
        this.author = user.displayName
        if (TextUtils.isEmpty(this.author)) {
            this.author = user.email
        }

        this.text = text
    }


}