package com.craiovadata.android.messenger.model

import com.google.firebase.firestore.IgnoreExtraProperties


@IgnoreExtraProperties
data class User(
        var email: String = "",
        var name: String = "",
        var photoUrl: String = ""
)