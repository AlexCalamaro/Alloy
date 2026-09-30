package com.squidink.alloy.lint

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.client.api.Vendor
import com.android.tools.lint.detector.api.CURRENT_API
import com.android.tools.lint.detector.api.Issue

class AlloyLintRegistry : IssueRegistry() {
    override val api: Int = CURRENT_API

    override val issues: List<Issue> = listOf(
        AlloyAntipatternDetector.RUN_BLOCKING_IN_PRODUCTION,
        AlloyAntipatternDetector.GLOBAL_SCOPE_USAGE
    )

    override val vendor: Vendor = Vendor(
        vendorName = "Alloy",
        feedbackUrl = "https://github.com/squidink/alloy/issues",
        contact = "alex@squidink.com"
    )
}
