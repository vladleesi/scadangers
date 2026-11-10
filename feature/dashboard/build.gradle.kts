plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose)
}
android {
    namespace = "dev.scadangers.feature.dashboard"
    buildFeatures { compose = true }
}
dependencies {
    implementation(project(":core:designsystem"))
    testImplementation(project(":core:testing"))
}
