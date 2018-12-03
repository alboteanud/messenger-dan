package com.craiovadata.android.messenger.model

//import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class User (
        var name: String? = "",
        var email: String? = ""

)

    //    var id: String? = null
//    var text: String? = null

//    var photoUrl: String? = null
//    var imageUrl: String? = null

//    constructor() {}
//
//    //    constructor(text: String, name: String, photoUrl: String, imageUrl: String) {
//    constructor(name: String, email: String) {
////        this.text = text
//        this.name = name
//        this.email = email
////        this.photoUrl = photoUrl
////        this.imageUrl = imageUrl
//    }
//
//}