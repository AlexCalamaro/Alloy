package com.squidink.alloy.modules.statspill

import androidx.compose.runtime.Composable
import com.squidink.alloy.core.design.FeatureDetailSection
import com.squidink.alloy.core.design.InfoCard
import com.squidink.alloy.core.design.SettingCard
import com.squidink.alloy.core.layout.FeatureDetail

/**
 * StatsPill feature detail content.
 * Settings are now handled directly in StatsScreen via DetailPaneScaffold.
 * This is kept for compatibility but shows only informational content.
 */
class StatsPillFeatureDetail : FeatureDetail {
    override val showsDetailPane: Boolean = true
    
    @Composable
    override fun DetailContent() {
        FeatureDetailSection("Stats Settings") {
            InfoCard(
                "Settings are available directly on the Stats screen via the settings button."
            )
        }
    }
}
