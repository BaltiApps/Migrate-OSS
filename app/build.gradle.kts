plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
}

val versionMajor = 6
val versionMinor = 0
val versionPatch = 2

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

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
    implementation(libs.com.google.guava)
    implementation(libs.androidx.documentfile)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}