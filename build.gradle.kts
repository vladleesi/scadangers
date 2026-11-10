import com.android.build.api.dsl.LibraryExtension

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.compose) apply false
    alias(libs.plugins.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}

subprojects {
    plugins.withId("com.android.library") {
        extensions.configure<LibraryExtension> {
            compileSdk = 37
            defaultConfig {
                minSdk = 24
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    }
}

// CLI isolation keeps stable Detekt's Kotlin compiler off AGP's Kotlin classpath.
val detektCli by configurations.creating
val ktlintCli by configurations.creating {
    // ktlint-cli publishes external and shadowed variants; use the self-contained CLI jar.
    attributes { attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.SHADOWED)) }
}

dependencies {
    detektCli(libs.detekt.cli)
    ktlintCli(libs.ktlint.cli)
}

tasks.register<JavaExec>("detekt") {
    classpath = detektCli
    mainClass.set("io.gitlab.arturbosch.detekt.cli.Main")
    args(
        "--input",
        "app/src,core,feature",
        "--config",
        "config/detekt.yml",
        "--build-upon-default-config"
    )
    args("--excludes", "**/build/**", "--report", "sarif:build/reports/detekt/report.sarif")
}

tasks.register<JavaExec>("ktlintCheck") {
    classpath = ktlintCli
    mainClass.set("com.pinterest.ktlint.Main")
    args("**/*.kt", "**/*.kts", "!**/build/**", "!**/.gradle/**")
}

tasks.register<JavaExec>("ktlintFormat") {
    classpath = ktlintCli
    mainClass.set("com.pinterest.ktlint.Main")
    args("--format", "**/*.kt", "**/*.kts", "!**/build/**", "!**/.gradle/**")
}
