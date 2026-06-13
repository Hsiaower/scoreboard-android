package com.hsiaower.scoreboard

import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.hsiaower.scoreboard.ui.ScoreboardApp

class MainActivity : ComponentActivity() {
    private val viewModel: ScoreboardViewModel by viewModels()
    private val handledKeyCodes = mutableSetOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()
        enterImmersiveMode()
        setContent {
            ScoreboardApp(viewModel = viewModel)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            enterImmersiveMode()
        } else {
            handledKeyCodes.clear()
            viewModel.releaseRemoteInputs()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (viewModel.handleKeyEvent(event) || keyCode in handledKeyCodes) {
            handledKeyCodes += keyCode
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        val handled = viewModel.handleKeyEvent(event)
        if (handledKeyCodes.remove(keyCode) || handled) return true
        return super.onKeyUp(keyCode, event)
    }

    private fun enterImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}
