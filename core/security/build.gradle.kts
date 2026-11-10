plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.serialization)
}
kotlin {
    jvmToolchain(17)
}
dependencies {
    api(libs.coroutines.core)
    implementation(libs.serialization)
    testImplementation(project(":core:testing"))
}
