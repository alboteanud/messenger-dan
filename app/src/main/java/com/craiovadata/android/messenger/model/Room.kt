package com.craiovadata.android.messenger.model

import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.*
import kotlin.collections.HashMap

@IgnoreExtraProperties
data class Room(
        var palName: String = "",
        var palId: String = "",
        var palPhotoUrl: String = "",
        var lastMsgAuthor: String? = "",
        var lastMsg: String? = "",
        @ServerTimestamp var created: Date? = null,
        @ServerTimestamp var lastMsgTime: Date? = null
//        var participants: List<HashMap<String, Any>> = listOf(),
) {

}
