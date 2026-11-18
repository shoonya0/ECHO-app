// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.spotless)
}

// Formatting: ktlint through Spotless. `ratchetFrom` limits spotlessCheck / spotlessApply to
// Kotlin files changed since origin/main, so existing code is reformatted only when a change
// touches it (never the whole repo at once). Style settings live in .editorconfig.
//   ./gradlew spotlessCheck   # part of `node scripts/verify.mjs`
//   ./gradlew spotlessApply   # fixes what spotlessCheck reports
spotless {
    ratchetFrom("origin/main")
    kotlin {
        target("app/src/**/*.kt")
        targetExclude("**/build/**")
        ktlint(libs.versions.ktlint.get())
    }
}
