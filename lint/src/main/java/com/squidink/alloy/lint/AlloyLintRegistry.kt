package com.squidink.alloy.lint

import com.android.tools.lint.detector.api.IssueRegistry
import com.android.tools.lint.detector.api.CURRENT_API
import com.android.tools.lint.detector.api.MIN_API

class AlloyLintRegistry : IssueRegistry() {
    override val api: Int = CURRENT_API
    override val minApi: Int = MIN_API

    override val issues: List<Issue> = listOf(
        COMPOSABLE_STATE_NOT_HOISTED,
        RUN_BLOCKING_IN_PRODUCTION,
        GLOBAL_SCOPE_USAGE,
        MISSING_KDOC_ON_PUBLIC_API,
        MAGIC_NUMBER_IN_PRODUCTION,
        MUTABLE_STATE_IN_COMPOSABLE
    )
}
