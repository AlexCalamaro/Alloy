package com.squidink.alloy.modules.scenes

import androidx.compose.runtime.Composable
import com.squidink.alloy.core.design.FeatureDetailSection
import com.squidink.alloy.core.design.InfoCard
import com.squidink.alloy.core.design.SettingCard
import com.squidink.alloy.core.layout.FeatureDetail

/**
 * Scenes feature detail content.
 * Provides scene management settings and configuration options.
 */
class ScenesFeatureDetail : FeatureDetail {
    override val showsDetailPane: Boolean = true
    
    @Composable
    override fun DetailContent() {
        FeatureDetailSection("Scene Settings") {
            SettingCard(
                title = "Scene Templates",
                subtitle = "Pre-configured window layouts for common workflows",
                value = "4 templates available"
            )
            
            SettingCard(
                title = "Auto-launch Apps",
                subtitle = "Automatically launch apps when scene is activated"
            )
            
            InfoCard("Scenes help you quickly set up your workspace for different tasks.")
        }
    }
}
