package com.craiovadata.android.messenger.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.*

data class Conversation(
        var uidP: String?,
        var nameP: String?,
        var emailP: String?,
        var author: String?,
        var text: String?,
        var photoUrlP: String?,
        @ServerTimestamp var timestamp: Date?
){
    constructor() : this(null, null, null, null, null, null, null)
}