package com.craiovadata.android.messenger.model

import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.*
import kotlin.collections.HashMap

@IgnoreExtraProperties
data class Room(
//        var id: String? = "",
        @ServerTimestamp var created: Date? = null,
        var lastMsgAuthor: String? = "",
        var lastMsg: String? = "",
        var participants: List<HashMap<String, Any>> = listOf(),
        @ServerTimestamp var lastMsgTime: Date? = null
)
