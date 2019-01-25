package com.craiovadata.android.messenger.model

import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.*

@IgnoreExtraProperties
data class Conversation(
        var palName: String = "",
        var palId: String = "",
        var palPhotoUrl: String = "",
        var lastMessageAuthor: String? = "",
        var lastMessage: String? = "",
        @ServerTimestamp var created: Date? = null,
        @ServerTimestamp var timestamp: Date? = null,
        var heBlockedMe: Boolean = false,
        var iBlockedHim: Boolean = false
//        var blockedMap: HashMap<String, Any> = HashMap()
)
