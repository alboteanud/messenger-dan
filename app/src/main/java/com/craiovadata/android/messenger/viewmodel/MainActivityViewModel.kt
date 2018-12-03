package com.craiovadata.android.messenger.viewmodel

import androidx.lifecycle.ViewModel
import com.craiovadata.android.messenger.Filters

/**
 * ViewModel for [com.google.firebase.example.fireeats.MainActivity].
 */

class MainActivityViewModel : ViewModel() {

    var isSigningIn: Boolean = false
    var filters: Filters = Filters.default
}
