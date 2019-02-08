package com.craiovadata.android.messenger

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import com.craiovadata.android.messenger.util.ForegroundListener

class DummyApp : Application() {

    override fun onCreate() {
        super.onCreate()
        ProcessLifecycleOwner.get()
                .lifecycle
                .addObserver(ForegroundListener())
    }

}