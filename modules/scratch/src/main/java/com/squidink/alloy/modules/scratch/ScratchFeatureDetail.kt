package com.squidink.alloy.modules.scratch

import androidx.compose.runtime.Composable
import com.squidink.alloy.core.design.FeatureDetailSection
import com.squidink.alloy.core.design.InfoCard
import com.squidink.alloy.core.design.SettingCard
import com.squidink.alloy.core.layout.FeatureDetail

/**
 * Scratch / Code Editor feature detail content.
 * Provides editor settings and security configuration information.
 */
class ScratchFeatureDetail : FeatureDetail {
    override val showsDetailPane: Boolean = true

    @Composable
    override fun DetailContent() {
        FeatureDetailSection("Editor & Storage Settings") {
            SettingCard(
                title = "Multi-Document Tabs",
                subtitle = "Manage multiple code and text files concurrently",
                value = "Active"
            )

            SettingCard(
                title = "Syntax Highlighting",
                subtitle = "Powered by SnipMe Highlights (17+ languages)",
                value = "Enabled"
            )

            SettingCard(
                title = "Secure Lockbox",
                subtitle = "Biometric + Screen Lock authentication for private documents",
                value = "Hardware AES-256"
            )

            SettingCard(
                title = "Auto-save",
                subtitle = "Debounced automatic persistence per document",
                value = "500ms"
            )

            InfoCard("Lockbox Policy: Documents in the Secure Lockbox automatically re-lock upon application restart or backgrounding.")
        }
    }
}
