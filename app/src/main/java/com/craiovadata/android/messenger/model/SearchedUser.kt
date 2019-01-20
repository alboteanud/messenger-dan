package com.craiovadata.android.messenger.model

import com.google.firebase.firestore.IgnoreExtraProperties


@IgnoreExtraProperties
data class SearchedUser(
        var uid: String = "",
        var name: String = "",
        var photoUrl: String = "",
        var keywords: MutableList<String> = mutableListOf()
)