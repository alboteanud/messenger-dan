package com.craiovadata.android.messenger.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.*


//@IgnoreExtraProperties
//data class Conversation(
//        var uidP: String = "",
//        var nameP: String = "",
//        var emailP: String = "",
//        var author: String = "",
//        var text: String = "",
//        var photoUrlP: String = ""
////        var timestamp: Date = Date()
//)

data class Conversation(
        var uidP: String? = null,
        var nameP: String? = null,
        var emailP: String? = null,
        var author: String? = null,
        var text: String? = null,
        var photoUrlP: String? = null,
        @ServerTimestamp var timestamp: Date? = null
)