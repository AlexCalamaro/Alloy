plugins {
    id("com.android.library")
    kotlin("jvm")
}

android {
    namespace = "com.squidink.alloy.lint"
    compileSdk = 37

    defaultConfig {
        minSdk = 21
    }

    sourceSets {
        named("main") {
            java.srcDir("src/main/java")
        }
    }
}

dependencies {
    implementation("com.android.tools.lint:lint-api:31.4.0")
    implementation("com.android.tools.lint:lint-checks:31.4.0")
    
    // Kotlin PSI
    implementation("org.jetbrains.kotlin:kotlin-compiler:2.0.21")
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.0.21")
    
    // UAST
    implementation("com.android.tools:uast:31.4.0")
}

// Configure JAR to be used as lint check
tasks.jar {
    manifest {
        attributes(
            "Lint-Registry-v2" to "com.squidink.alloy.lint.AlloyLintRegistry"
        )
    }
}
