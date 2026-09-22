package com.squidink.alloy.modules.scratch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.squidink.alloy.core.design.AlloyTheme
import com.squidink.alloy.modules.scratch.ui.ScratchScreen
import dagger.hilt.android.AndroidEntryPoint

/**
 * Standalone multi-instance scratchpad window Activity.
 */
@AndroidEntryPoint
class ScratchActivity : ComponentActivity() {

    private val viewModel: ScratchViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AlloyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ScratchScreen(viewModel = viewModel)
                }
            }
        }
    }
}
