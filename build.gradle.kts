// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.detekt)
}

detekt {
    toolVersion = libs.versions.detekt.get()
    config.setFrom(files("${rootProject.projectDir}/detekt.yml"))
    baseline = file("${rootProject.projectDir}/detekt-baseline.xml")
    buildUponDefaultConfig = true
    source.setFrom(
        files(
            "app/src/main/java",
            "core/common/src/main/java",
            "core/data/src/main/java",
            "core/design/src/main/java",
            "core/datastore/src/main/java",
            "core/domain/src/main/java",
            "core/layout/src/main/java",
            "core/navigation/src/main/java",
            "core/permissions/src/main/java",
            "core/proc/src/main/java",
            "modules/statspill/src/main/java",
            "modules/scratch/src/main/java",
            "modules/lists/src/main/java",
            "modules/rssreader/src/main/java",
            "modules/scenes/src/main/java",
            "modules/settings/src/main/java",
            "modules/llmhost/src/main/java"
        )
    )
}
