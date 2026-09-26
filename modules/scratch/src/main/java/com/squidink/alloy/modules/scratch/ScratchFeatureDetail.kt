package com.squidink.alloy.modules.scratch

import androidx.compose.runtime.Composable
import com.squidink.alloy.core.design.FeatureDetailSection
import com.squidink.alloy.core.design.InfoCard
import com.squidink.alloy.core.design.SettingCard
import com.squidink.alloy.core.layout.FeatureDetail

/**
 * Scratch feature detail content.
 * Provides scratchpad settings and configuration options.
 */
class ScratchFeatureDetail : FeatureDetail {
    override val showsDetailPane: Boolean = true
    
    @Composable
    override fun DetailContent() {
        FeatureDetailSection("Scratchpad Settings") {
            SettingCard(
                title = "Auto-save",
                subtitle = "Automatically save notes as you type",
                value = "Enabled"
            )
            
            SettingCard(
                title = "Text Formatting",
                subtitle = "Support for Markdown syntax"
            )
            
            InfoCard("Your notes are stored locally and encrypted.")
        }
    }
}
