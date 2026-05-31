import java.util.Base64
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
}

val versionMajor = 6
val versionMinor = 2
val versionPatch = 0

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

        val schema = "migrate"
        val hostProgressBackup = "backup_progress"
        val hostProgressRestore = "restore_progress"

        manifestPlaceholders["schema"] = schema
        manifestPlaceholders["hostProgressBackup"] = hostProgressBackup
        manifestPlaceholders["hostProgressRestore"] = hostProgressRestore

        buildConfigField("String", "SCHEMA", "\"${schema}\"")
        buildConfigField("String", "HOST_PROGRESS_BACKUP", "\"${hostProgressBackup}\"")
        buildConfigField("String", "HOST_PROGRESS_RESTORE", "\"${hostProgressRestore}\"")

        setProperty("archivesBaseName", "Migrate-v${versionMajor}.${versionMinor}.${versionPatch}")
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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
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
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}