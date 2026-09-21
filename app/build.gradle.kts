import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.File
import java.security.MessageDigest
import java.util.Properties

fun gitBytes(root: File, vararg arguments: String): ByteArray? = runCatching {
    val exec = providers.exec {
        workingDir(root)
        commandLine("git", *arguments)
        isIgnoreExitValue = true
    }
    val result = exec.result.get()
    if (result.exitValue == 0) {
        exec.standardOutput.asBytes.get()
    } else {
        logger.warn("Git metadata command failed with exit code ${result.exitValue}")
        null
    }
}.onFailure {
    logger.warn("Git metadata command could not run: ${it.javaClass.simpleName}")
}.getOrNull()

fun sha256(parts: List<ByteArray>): String {
    val digest = MessageDigest.getInstance("SHA-256")
    parts.forEach(digest::update)
    return digest.digest().joinToString("") { "%02x".format(it) }
}

val localProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
}
val amapApiKey = localProperties.getProperty("AMAP_API_KEY", "")
val releaseSigningProperties = Properties().apply {
    rootProject.file("release-signing.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
}
fun releaseSigningValue(name: String): String? = providers.environmentVariable(name).orNull
    ?: releaseSigningProperties.getProperty(name)
val releaseStoreFile = releaseSigningValue("RELEASE_STORE_FILE")
val releaseStorePassword = releaseSigningValue("RELEASE_STORE_PASSWORD")
val releaseKeyAlias = releaseSigningValue("RELEASE_KEY_ALIAS")
val releaseKeyPassword = releaseSigningValue("RELEASE_KEY_PASSWORD") ?: releaseStorePassword
val releaseSigningConfigured = listOf(
    releaseStoreFile, releaseStorePassword, releaseKeyAlias, releaseKeyPassword,
).all { !it.isNullOrBlank() }
val gitShaBytes = gitBytes(rootDir, "rev-parse", "HEAD")
val buildGitSha = gitShaBytes?.decodeToString()?.trim()?.takeIf { it.isNotEmpty() } ?: "UNAVAILABLE"
val gitStatus = gitBytes(rootDir, "status", "--porcelain=v1", "-z")
val sourceState = when {
    gitShaBytes == null || gitStatus == null -> "UNAVAILABLE"
    gitStatus.isEmpty() -> "CLEAN"
    else -> "DIRTY:${sha256(listOf(gitStatus))}"
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
}
room {
    schemaDirectory("$projectDir/schemas")
}

android {
    namespace = "com.yangchengwei.easytrip"
    compileSdk = 36
    buildToolsVersion = "36.0.0"
    defaultConfig {
        applicationId = "com.yangchengwei.easytrip"
        minSdk = 26
        targetSdk = 36
        versionCode = 6
        versionName = "1.4.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["AMAP_API_KEY"] = amapApiKey
        buildConfigField("String", "GIT_SHA", "\"$buildGitSha\"")
        buildConfigField("String", "SOURCE_STATE", "\"$sourceState\"")
    }
    signingConfigs {
        if (releaseSigningConfigured) {
            create("release") {
                storeFile = rootProject.file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }
    buildTypes {
        release {
            isDebuggable = false
            isMinifyEnabled = false
            if (releaseSigningConfigured) signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions { unitTests.isIncludeAndroidResources = true }
}
dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.amap.combined)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

// Fail before producing a release artifact if its signing or map configuration is missing.
tasks.matching { it.name == "preReleaseBuild" }.configureEach {
    doFirst {
        check(releaseSigningConfigured) {
            "Release signing is required. Configure release-signing.properties or RELEASE_* environment variables."
        }
        check(rootProject.file(releaseStoreFile!!).isFile) { "Release keystore file was not found." }
        check(amapApiKey.isNotBlank()) { "AMAP_API_KEY must be configured for release builds." }
    }
}
