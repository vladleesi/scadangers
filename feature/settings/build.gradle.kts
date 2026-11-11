plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose)
}
android {
    namespace = "dev.vladleesi.scadangers.feature.settings"
    buildFeatures { compose = true }
}
dependencies {
    implementation(project(":core:designsystem"))
}
