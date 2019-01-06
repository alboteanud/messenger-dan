package com.craiovadata.android.messenger.util

import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent

class ForegroundBackgroundListener : LifecycleObserver {

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onStart() {
        Log.v("ProcessLog", "APP IS ON FOREGROUND")
        Companion.active = true
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onStop() {
        Log.v("ProcessLog", "APP IS IN BACKGROUND")

        Companion.active = false
    }


    companion object {
        var active = false
        fun isForeground(): Boolean { return active}
    }

}