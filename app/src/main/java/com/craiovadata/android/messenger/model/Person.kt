package com.craiovadata.android.messenger.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.*

data class Person(
        var uid: String?,
        var name: String?,
        var email: String?,
        var statusTxt: String?,
        var photoUrl: String?,
        @ServerTimestamp var timestamp: Date?
){
    constructor() : this(null, null, null, null, null, null)

    companion object {
        const val STATUS_TXT = "statusTxt"
    }
}
