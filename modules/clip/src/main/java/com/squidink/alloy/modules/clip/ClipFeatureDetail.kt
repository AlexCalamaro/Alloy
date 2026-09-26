package com.squidink.alloy.modules.clip

import androidx.compose.runtime.Composable
import com.squidink.alloy.core.design.FeatureDetailSection
import com.squidink.alloy.core.design.InfoCard
import com.squidink.alloy.core.design.SettingCard
import com.squidink.alloy.core.layout.FeatureDetail

/**
 * Clip feature detail content.
 * Provides clipboard settings and configuration options.
 */
class ClipFeatureDetail : FeatureDetail {
    override val showsDetailPane: Boolean = true
    
    @Composable
    override fun DetailContent() {
        FeatureDetailSection("Clipboard Settings") {
            SettingCard(
                title = "History Size",
                subtitle = "Number of clips to retain in history",
                value = "50 clips"
            )
            
            SettingCard(
                title = "Pinned Clips",
                subtitle = "Pinned clips are never removed from history"
            )
            
            InfoCard("Tip: Long-press a clip to pin it or edit its content.")
        }
    }
}
