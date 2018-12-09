package com.craiovadata.android.messenger.model

import com.google.firebase.firestore.IgnoreExtraProperties
import java.util.*
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

@IgnoreExtraProperties
data class Room(
        var id: String? = "",
        @ServerTimestamp var created: Date? = null,
        var author: String? = "",
        var lastMsg: String? = "",
        var participants: String? = "",
        @ServerTimestamp var msgTimestamp: Date? = null
) {

}
