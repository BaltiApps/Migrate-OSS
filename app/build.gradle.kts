import java.util.Base64
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
}

val versionMajor = 6
val versionMinor = 2
val versionPatch = 3

fun getEnvValue(key: String): String? {
    val envData = System.getenv(key)
    if (envData != null) return envData

    val properties = Properties()
    val propertiesFile = project.rootProject.file("local.properties")
    if (propertiesFile.exists()) {
        propertiesFile.inputStream().use {
            properties.load(it)
        }
    }
    return properties.getProperty(key)
}

fun getKeyStoreFile(base64String: String): File {
    val keystoreDir = File(rootDir, "keystore")
    val keystoreFile = File(keystoreDir, "release-key.jks")
    keystoreDir.mkdirs()
    val decoded = Base64.getDecoder().decode(base64String)
    keystoreFile.writeBytes(decoded)
    return keystoreFile
}

android {
    namespace = "balti.migrate"
    compileSdk = 36

    defaultConfig {
        applicationId = "balti.migrate"
        minSdk = 30
        targetSdk = 36
        versionCode = versionMajor * 10000 + versionMinor * 100 + versionPatch
        versionName = "${versionMajor}.${versionMinor}.${versionPatch}-beta"
        buildConfigField("String", "VERSION_CODENAME", "\"Artemis\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        val contentProviderAuthority = "$applicationId.fileProviderAuthority"
        manifestPlaceholders["contentProviderAuthority"] = contentProviderAuthority
        buildConfigField("String", "contentProviderAuthority", "\"${contentProviderAuthority}\"")

        val admobAppId = getEnvValue("ADMOB_APP_ID")?.trim() ?: ""
        manifestPlaceholders["admobAppId"] = admobAppId.ifBlank { "ca-app-pub-3940256099942544~3347511713" }
        buildConfigField("String", "ADMOB_APP_ID", "\"${admobAppId}\"")

        val admobBannerUnitIdHome = getEnvValue("ADMOB_BANNER_UNIT_ID_HOME")?.trim() ?: ""
        buildConfigField("String", "ADMOB_BANNER_UNIT_ID_HOME", "\"${admobBannerUnitIdHome}\"")

        val admobBannerIdBackupProgress = getEnvValue("ADMOB_BANNER_ID_BACKUP_PROGRESS")?.trim() ?: ""
        buildConfigField("String", "ADMOB_BANNER_ID_BACKUP_PROGRESS", "\"${admobBannerIdBackupProgress}\"")

        val admobBannerIdRestoreProgress = getEnvValue("ADMOB_BANNER_ID_RESTORE_PROGRESS")?.trim() ?: ""
        buildConfigField("String", "ADMOB_BANNER_ID_RESTORE_PROGRESS", "\"${admobBannerIdRestoreProgress}\"")

        val consentDebugDeviceId = getEnvValue("CONSENT_DEBUG_DEVICE_ID")?.trim() ?: ""
        buildConfigField("String", "CONSENT_DEBUG_DEVICE_ID", "\"${consentDebugDeviceId}\"")

        val schema = "migrate"
        val hostProgressBackup = "backup_progress"
        val hostProgressRestore = "restore_progress"

        manifestPlaceholders["schema"] = schema
        manifestPlaceholders["hostProgressBackup"] = hostProgressBackup
        manifestPlaceholders["hostProgressRestore"] = hostProgressRestore

        buildConfigField("String", "SCHEMA", "\"${schema}\"")
        buildConfigField("String", "HOST_PROGRESS_BACKUP", "\"${hostProgressBackup}\"")
        buildConfigField("String", "HOST_PROGRESS_RESTORE", "\"${hostProgressRestore}\"")
    }

    signingConfigs {
        create("release") {
            storeFile = getKeyStoreFile(getEnvValue("KEYSTORE_BASE64") ?: "")
            storePassword = getEnvValue("KEYSTORE_PASSWORD")
            keyAlias = getEnvValue("KEY_ALIAS")
            keyPassword = getEnvValue("KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    jvmToolchain(25)
}

base {
    archivesName.set("Migrate-v${versionMajor}.${versionMinor}.${versionPatch}")
}

dependencies {

    implementation(project(":domain"))
    implementation(libs.coroutines.core)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.extendedicons)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.koin.coroutines)
    implementation(libs.koin.compose)
    implementation(libs.koin.compose.viewmodel)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.com.jakewharton.timber)
    implementation(libs.androidx.documentfile)
    implementation(libs.billing)
    implementation(libs.ads.mobile.sdk)
    implementation(libs.ump)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}