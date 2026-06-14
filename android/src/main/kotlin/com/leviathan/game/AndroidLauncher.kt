package com.leviathan.game

import android.os.Bundle
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration

class AndroidLauncher : AndroidApplication() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val config = AndroidApplicationConfiguration().apply {
            useAccelerometer = false
            useCompass = false
            useGyroscope = false
            numSamples = 2
            useWakelock = true
            hideStatusBar = true
        }
        initialize(LeviathanGame(), config)
    }
}
