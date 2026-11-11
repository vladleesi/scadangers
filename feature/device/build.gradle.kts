plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose)
}
android {
    namespace = "dev.vladleesi.scadangers.feature.device"
    buildFeatures { compose = true }
}
dependencies {
    implementation(project(":core:security"))
    implementation(project(":core:common"))
    implementation(project(":core:designsystem"))
    testImplementation(project(":core:testing"))
}
