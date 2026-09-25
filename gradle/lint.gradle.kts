# Android Lint Configuration for Alloy Project
# Custom rules to enforce best practices

lint {
    // Enable all checks
    checkAll = true
    
    // Fail on errors
    abortOnError = true
    
    // Treat all warnings as errors - no warnings allowed without justification
    warningsAsErrors = true
    
    // Ignore warnings for test sources
    ignoreTestSources = true
    
    // Disable specific checks
    disable += [
        // AndroidX migration warnings
        "RestrictedApi",
        "SyntheticAccessor",
        
        // Gradle plugin warnings
        "GradleDependency",
        "OldTargetApi",
        
        // Performance (be conservative)
        "UnusedResources",
        "UnusedAttribute",
        
        // Typography (not critical)
        "TypographyQuotes",
        "TypographyDashes",
        "TypographyEllipsis",
        "TypographyOther",
        
        // Usability (opinionated)
        "IconDensities",
        "IconExpectedSize",
        "ContentDescription",
    ]
    
    // Enable specific checks for antipatterns
    enable += [
        // Memory leak detection
        "LeakCanary",
        "StaticFieldLeak",
        "HandlerLeak",
        "ViewConstructor",
        
        // Compose stability
        "ComposeModifierMissing",
        "ComposeNaming",
        "ComposeUnstableCollections",
        "ComposeRememberMissing",
        
        // Coroutines
        "CoroutineScope",
        "UnsafeCallOnNullableType",
        
        // General quality
        "UnusedImport",
        "RedundantVisibilityModifier",
        "MagicNumber",
        "LongMethod",
        "LargeClass",
        "ComplexMethod",
        
        // Documentation
        "MissingKDoc",
        "InvalidPackageDeclaration",
        
        // Security
        "HardcodedText",
        "InvalidPackageDeclaration",
        
        // Database
        "RoomMigration",
        "UnusedPrimaryKey",
        "MissingIndex",
    ]
    
    // Warnings as errors for critical issues
    warningsAsErrors += [
        "LeakCanary",
        "StaticFieldLeak",
        "HandlerLeak",
        "CoroutineScope",
        "ComposeModifierMissing",
        "ComposeNaming",
        "ComposeRememberMissing",
        "MissingKDoc",
        "MissingIndex",
    ]
}

// Custom lint baseline for existing issues
baseline {
    file = "lint-baseline.xml"
}
