package com.craiovadata.android.messenger.model

import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.*

@IgnoreExtraProperties
data class Conversation(
        var palName: String = "",
        var palId: String = "",
        var palPhotoUrl: String = "",
        var msgAuthor: String? = "",
        var lastMessage: String? = "",
        @ServerTimestamp var created: Date? = null,
        @ServerTimestamp var msgTimestamp: Date? = null
//        var participants: List<HashMap<String, Any>> = listOf(),
)
