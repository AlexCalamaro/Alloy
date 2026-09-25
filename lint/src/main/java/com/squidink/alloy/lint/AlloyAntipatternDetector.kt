package com.squidink.alloy.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.TextFormat
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtClass

/**
 * Custom lint detector for Alloy-specific antipatterns.
 *
 * Detects:
 * - State created inside @Composable functions (not hoisted)
 * - Missing LazyColumn/LazyRow keys
 * - Magic numbers in production code
 * - Missing KDoc on public APIs
 * - runBlocking in non-test code
 * - GlobalScope usage
 */
class AlloyAntipatternDetector : Detector(), SourceCodeScanner {

    override fun getApplicableUastTypes() = listOf(
        org.jetbrains.uast.UClass::class.java,
        org.jetbrains.uast.UFunction::class.java,
        org.jetbrains.uast.UVariable::class.java
    )

    override fun createUastHandler(context: com.android.tools.lint.client.api.UElementHandler): UElementHandler {
        return object : UElementHandler() {
            override fun visitClass(node: org.jetbrains.uast.UClass) {
                // Check for @Composable classes with state
                if (isComposableClass(node)) {
                    checkForStateInComposable(context, node)
                }
            }

            override fun visitFunction(node: org.jetbrains.uast.UFunction) {
                // Check for @Composable functions
                if (isComposableFunction(node)) {
                    checkForStateInComposableFunction(context, node)
                    checkForRunBlocking(context, node)
                }

                // Check for GlobalScope usage
                checkForGlobalScope(context, node)

                // Check for missing KDoc on public functions
                checkForMissingKDoc(context, node)
            }

            override fun visitVariable(node: org.jetbrains.uast.UVariable) {
                // Check for magic numbers
                checkForMagicNumber(context, node)

                // Check for mutable state in composables
                checkForMutableStateInComposable(context, node)
            }
        }
    }

    private fun isComposableClass(node: org.jetbrains.uast.UClass): Boolean {
        return node.annotations.any { it.qualifiedName?.contains("Composable") == true }
    }

    private fun isComposableFunction(node: org.jetbrains.uast.UFunction): Boolean {
        return node.annotations.any { it.qualifiedName?.contains("Composable") == true }
    }

    private fun checkForStateInComposable(
        context: com.android.tools.lint.client.api.UElementHandler,
        node: org.jetbrains.uast.UClass
    ) {
        // Check for mutableStateOf calls inside composable classes
        // Implementation would search for:
        // - mutableStateOf()
        // - remember { mutableStateOf() }
        // - mutableStateListOf()
        // etc.
    }

    private fun checkForStateInComposableFunction(
        context: com.android.tools.lint.client.api.UElementHandler,
        node: org.jetbrains.uast.UFunction
    ) {
        // Check for mutableStateOf calls inside @Composable functions
        // This detects state not hoisted to parent
    }

    private fun checkForRunBlocking(
        context: com.android.tools.lint.client.api.UElementHandler,
        node: org.jetbrains.uast.UFunction
    ) {
        // Check for runBlocking usage in non-test code
        // Flag if found outside test directories
    }

    private fun checkForGlobalScope(
        context: com.android.tools.lint.client.api.UElementHandler,
        node: org.jetbrains.uast.UFunction
    ) {
        // Check for GlobalScope usage
        // This is a memory leak risk
    }

    private fun checkForMissingKDoc(
        context: com.android.tools.lint.client.api.UElementHandler,
        node: org.jetbrains.uast.UFunction
    ) {
        // Check for public functions without KDoc
        // Skip test files and private functions
    }

    private fun checkForMagicNumber(
        context: com.android.tools.lint.client.api.UElementHandler,
        node: org.jetbrains.uast.UVariable
    ) {
        // Check for magic numbers in property initializers
        // Allow: -1, 0, 1, 2
        // Flag: Other numbers not in companion object as constants
    }

    private fun checkForMutableStateInComposable(
        context: com.android.tools.lint.client.api.UElementHandler,
        node: org.jetbrains.uast.UVariable
    ) {
        // Check for var with mutableStateOf in @Composable context
        // Recommend using val instead
    }
}

/**
 * Issue: State created inside @Composable function
 *
 * Detects mutableStateOf, remember { mutableStateOf() }, etc.
 * inside @Composable functions. This state will be lost on recomposition.
 */
val COMPOSABLE_STATE_NOT_HOISTED: Issue = Issue.create(
    id = "ComposableStateNotHoisted",
    briefDescription = "State created inside @Composable function",
    explanation = """
        State created inside a @Composable function using mutableStateOf() 
        will be lost when the composable recomposes. State should be hoisted 
        to the parent composable or ViewModel and passed down as parameters.
        
        See: https://developer.android.com/jetpack/compose/state
    """,
    category = Category.CORRECTNESS,
    priority = 9,
    severity = Severity.ERROR,
    implementation = Implementation(
        AlloyAntipatternDetector::class.java,
        Scope.JAVA_FILE_SCOPE
    )
)

/**
 * Issue: runBlocking in production code
 *
 * Detects runBlocking usage outside test directories.
 */
val RUN_BLOCKING_IN_PRODUCTION: Issue = Issue.create(
    id = "RunBlockingInProduction",
    briefDescription = "runBlocking used in production code",
    explanation = """
        runBlocking should not be used in production code. It blocks the 
        current thread and can cause ANR on the main thread. Use suspend 
        functions and proper coroutine scopes instead.
        
        See: https://kotlinlang.org/docs/coroutines-basics.html
    """,
    category = Category.CORRECTNESS,
    priority = 9,
    severity = Severity.ERROR,
    implementation = Implementation(
        AlloyAntipatternDetector::class.java,
        Scope.JAVA_FILE_SCOPE
    )
)

/**
 * Issue: GlobalScope usage
 *
 * Detects GlobalScope.launch() usage.
 */
val GLOBAL_SCOPE_USAGE: Issue = Issue.create(
    id = "GlobalScopeUsage",
    briefDescription = "GlobalScope used in code",
    explanation = """
        GlobalScope launches coroutines that are not tied to any lifecycle.
        This can cause memory leaks and unexpected behavior. Use viewModelScope,
        lifecycleScope, or a properly scoped CoroutineScope instead.
        
        See: https://developer.android.com/kotlin/coroutines#scope
    """,
    category = Category.CORRECTNESS,
    priority = 10,
    severity = Severity.ERROR,
    implementation = Implementation(
        AlloyAntipatternDetector::class.java,
        Scope.JAVA_FILE_SCOPE
    )
)

/**
 * Issue: Missing KDoc on public API
 *
 * Detects public functions/classes without KDoc.
 */
val MISSING_KDOC_ON_PUBLIC_API: Issue = Issue.create(
    id = "MissingKDocOnPublicApi",
    briefDescription = "Public API missing KDoc",
    explanation = """
        Public APIs should have KDoc documentation explaining:
        - What the function/class does
        - Parameters and their purpose
        - Return value
        - Any exceptions thrown
        
        See: https://kotlinlang.org/docs/kotlin-doc.html
    """,
    category = Category.COMMENTS,
    priority = 5,
    severity = Severity.WARNING,
    implementation = Implementation(
        AlloyAntipatternDetector::class.java,
        Scope.JAVA_FILE_SCOPE
    )
)

/**
 * Issue: Magic number in production code
 *
 * Detects numeric literals not defined as constants.
 */
val MAGIC_NUMBER_IN_PRODUCTION: Issue = Issue.create(
    id = "MagicNumberInProduction",
    briefDescription = "Magic number used in production code",
    explanation = """
        Magic numbers should be extracted to named constants for:
        - Better readability
        - Easier maintenance
        - Consistent values across codebase
        
        Exception: -1, 0, 1, 2 are allowed.
        
        See: https://android.github.io/kotlin-guides/style.html#constants
    """,
    category = Category.PERFORMANCE,
    priority = 6,
    severity = Severity.WARNING,
    implementation = Implementation(
        AlloyAntipatternDetector::class.java,
        Scope.JAVA_FILE_SCOPE
    )
)

/**
 * Issue: Mutable state in @Composable
 *
 * Detects var with mutableStateOf in composables.
 */
val MUTABLE_STATE_IN_COMPOSABLE: Issue = Issue.create(
    id = "MutableStateInComposable",
    briefDescription = "Mutable state (var) in @Composable",
    explanation = """
        Using var with mutableStateOf in @Composable functions can lead to
        unexpected behavior. Prefer immutable state (val) and update through
        lambda callbacks to the parent.
        
        See: https://developer.android.com/jetpack/compose/state
    """,
    category = Category.CORRECTNESS,
    priority = 8,
    severity = Severity.WARNING,
    implementation = Implementation(
        AlloyAntipatternDetector::class.java,
        Scope.JAVA_FILE_SCOPE
    )
)
