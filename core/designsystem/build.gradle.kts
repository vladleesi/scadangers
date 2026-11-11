plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose)
}
android {
    namespace = "dev.vladleesi.scadangers.core.designsystem"
    buildFeatures { compose = true }
}
dependencies {
    constraints {
        // Standalone feature lint must resolve the same current Lifecycle checks as the app.
        api(libs.lifecycle.runtime)
    }
    api(libs.compose.ui)
    api(libs.compose.foundation)
    api(libs.material3)
    // Preview annotations are source-only metadata; R8 removes unused preview functions.
    api(libs.compose.ui.tooling.preview)
    // Renders previews in Android Studio. Debug builds only; the app debug manifest
    // removes its exported PreviewActivity.
    debugApi(libs.compose.ui.tooling)
    testImplementation(project(":core:testing"))
}
