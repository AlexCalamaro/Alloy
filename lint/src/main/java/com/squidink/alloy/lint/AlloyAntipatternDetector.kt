package com.squidink.alloy.lint

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.USimpleNameReferenceExpression

/**
 * Custom lint detector for Alloy-specific antipatterns:
 * - Flags `runBlocking` in production code
 * - Flags `GlobalScope` usage in production code
 */
class AlloyAntipatternDetector : Detector(), SourceCodeScanner {

    override fun getApplicableMethodNames(): List<String> = listOf("runBlocking")

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (context.isTestSource) return

        if (method.containingClass?.qualifiedName?.startsWith("kotlinx.coroutines") == true) {
            context.report(
                RUN_BLOCKING_IN_PRODUCTION,
                node,
                context.getLocation(node),
                "runBlocking should not be used in production code outside tests."
            )
        }
    }

    override fun getApplicableReferenceNames(): List<String> = listOf("GlobalScope")

    override fun visitReference(
        context: JavaContext,
        reference: org.jetbrains.uast.UReferenceExpression,
        referenced: PsiElement
    ) {
        if (context.isTestSource) return

        if ((referenced as? com.intellij.psi.PsiClass)?.qualifiedName == "kotlinx.coroutines.GlobalScope" ||
            reference.asSourceString() == "GlobalScope"
        ) {
            context.report(
                GLOBAL_SCOPE_USAGE,
                reference,
                context.getLocation(reference),
                "GlobalScope usage is discouraged; use an injected or lifecycle-bound CoroutineScope."
            )
        }
    }

    companion object {
        val RUN_BLOCKING_IN_PRODUCTION: Issue = Issue.create(
            id = "RunBlockingInProduction",
            briefDescription = "runBlocking used in production code",
            explanation = """
                runBlocking should not be used in production code. It blocks the 
                current thread and can cause ANRs. Use suspend functions and proper 
                coroutine scopes instead.
            """.trimIndent(),
            category = Category.CORRECTNESS,
            priority = 9,
            severity = Severity.ERROR,
            implementation = Implementation(
                AlloyAntipatternDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )

        val GLOBAL_SCOPE_USAGE: Issue = Issue.create(
            id = "GlobalScopeUsage",
            briefDescription = "GlobalScope used in code",
            explanation = """
                GlobalScope launches coroutines that are not tied to any lifecycle.
                This can cause memory leaks and unexpected behavior.
            """.trimIndent(),
            category = Category.CORRECTNESS,
            priority = 8,
            severity = Severity.WARNING,
            implementation = Implementation(
                AlloyAntipatternDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )
    }
}
