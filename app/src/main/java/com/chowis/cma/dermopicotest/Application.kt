package com.chowis.cma.dermopicotest

import android.app.Application
import com.chibatching.kotpref.Kotpref
import timber.log.Timber

class Application : Application() {

    override fun onCreate() {
        super.onCreate()

        instance = this
        if (BuildConfig.DEBUG) {

            Timber.plant(object : Timber.DebugTree() {
                // prepend log with class name, function name and line number
                override fun createStackElementTag(element: StackTraceElement): String? {
                    return String.format("[%s##%s:%s]", super.createStackElementTag(element), element.methodName, element.lineNumber)
                }
            })
        }

        Kotpref.init(this)
    }

    companion object {

        private var instance: Application? = null

        fun appContext(): Application {
            return instance as Application
        }
    }
}