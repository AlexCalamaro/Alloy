plugins {
    kotlin("jvm")
}

dependencies {
    compileOnly("com.android.tools.lint:lint-api:31.4.0")
    compileOnly("com.android.tools.lint:lint-checks:31.4.0")
}

// Configure JAR to be used as lint check
tasks.jar {
    manifest {
        attributes(
            "Lint-Registry-v2" to "com.squidink.alloy.lint.AlloyLintRegistry"
        )
    }
}
