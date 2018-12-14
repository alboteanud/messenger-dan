package com.craiovadata.android.messenger.model

import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.*

@IgnoreExtraProperties
data class Room(
        var id: String? = "",
        @ServerTimestamp var created: Date? = null,
        var author: String? = "",
        var lastMsg: String? = "",
        var participants: MutableList<String> = mutableListOf(),
        @ServerTimestamp var msgTimestamp: Date? = null
) {

}
