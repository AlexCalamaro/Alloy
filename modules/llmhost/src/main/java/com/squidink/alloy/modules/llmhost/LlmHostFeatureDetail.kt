package com.squidink.alloy.modules.llmhost

import androidx.compose.runtime.Composable
import com.squidink.alloy.core.design.FeatureDetailSection
import com.squidink.alloy.core.design.InfoCard
import com.squidink.alloy.core.layout.FeatureDetail

/**
 * Feature detail integration for the LLM Host module.
 */
class LlmHostFeatureDetail : FeatureDetail {
    override val showsDetailPane: Boolean = true

    @Composable
    override fun DetailContent() {
        FeatureDetailSection("LLM Host Info") {
            InfoCard(
                "On-device LiteRT LLM host. Provides standard OpenAI-compatible endpoints on " +
                    "127.0.0.1 for local applications without requiring cloud connections."
            )
        }
    }
}
