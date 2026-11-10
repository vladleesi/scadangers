plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
}
android {
    namespace = "dev.scadangers.core.database"
}
dependencies {
    api(libs.room.runtime)
    implementation(project(":core:security"))
    implementation(libs.serialization)
    implementation(libs.coroutines.core)
    ksp(libs.room.compiler)
}

ksp { arg("room.schemaLocation", "$projectDir/schemas") }
