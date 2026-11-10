import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.UnresolvedDependencyResult

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose)
    alias(libs.plugins.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}
android {
    namespace = "dev.scadangers"
    compileSdk = 37
    defaultConfig {
        applicationId = "dev.scadangers"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    buildFeatures {
        compose = true
        // Supplies the version shown in Settings without a PackageManager call on Main.
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    lint {
        abortOnError = true
        warningsAsErrors = true
        checkDependencies = true
    }
}
dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:database"))
    implementation(project(":core:security"))
    implementation(project(":feature:dashboard"))
    implementation(project(":feature:apps"))
    implementation(project(":feature:device"))
    implementation(project(":feature:network"))
    implementation(project(":feature:nearby"))
    implementation(project(":feature:alerts"))
    implementation(project(":feature:settings"))
    implementation(libs.androidx.core)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.compose)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.navigation.compose)
    implementation(libs.serialization)
    implementation(libs.coroutines.android)
    implementation(libs.hilt.android)
    implementation(libs.datastore)
    ksp(libs.hilt.compiler)
    testImplementation(project(":core:testing"))
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.runner)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.compose.test)
    debugImplementation(libs.compose.test.manifest)
}

// Audit the resolved transitive graph, including platform-specific runtime variants.
tasks.register("dependencyInventory") {
    val output = layout.buildDirectory.file("reports/dependencies/coordinates.txt")
    outputs.file(output)
    outputs.upToDateWhen { false }
    doLast {
        val coordinates = listOf(
            "debugRuntimeClasspath",
            "releaseRuntimeClasspath"
        ).flatMap { name ->
            val resolution = configurations.getByName(name).incoming.resolutionResult
            val failures = resolution.allDependencies.filterIsInstance<UnresolvedDependencyResult>()
            check(failures.isEmpty()) {
                "Dependency resolution failed: ${failures.map { it.attempted }}"
            }
            resolution.allComponents.mapNotNull {
                (it.id as? ModuleComponentIdentifier)?.let { id ->
                    "${id.group}:${id.module}:${id.version}"
                }
            }
        }.distinct().sorted()
        output.get().asFile.apply {
            parentFile.mkdirs()
            writeText(coordinates.joinToString("\n", postfix = "\n"))
        }
    }
}
