plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kover)
}

android {
    namespace = "com.matheus.callmanager"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.matheus.callmanager"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        create("release") {
            val storeFilePath = providers.gradleProperty("CALLMANAGER_RELEASE_STORE_FILE").orNull
            if (storeFilePath != null) {
                storeFile = file(storeFilePath)
                storePassword = providers.gradleProperty("CALLMANAGER_RELEASE_STORE_PASSWORD").get()
                keyAlias = providers.gradleProperty("CALLMANAGER_RELEASE_KEY_ALIAS").get()
                keyPassword = providers.gradleProperty("CALLMANAGER_RELEASE_KEY_PASSWORD").get()
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (providers.gradleProperty("CALLMANAGER_RELEASE_STORE_FILE").isPresent) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    lint {
        warningsAsErrors = true
        abortOnError = true
        checkDependencies = true
        baseline = file("lint-baseline.xml")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.libphonenumber.android)

    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.kotlinx.coroutines.test)
}

kover {
    currentProject {
        instrumentation {
            excludedClasses.addAll(
                "com.matheus.callmanager.BuildConfig",
                "com.matheus.callmanager.*Hilt*",
                "com.matheus.callmanager.*_Factory",
                "com.matheus.callmanager.*_MembersInjector",
            )
        }
    }
    reports {
        filters {
            excludes {
                annotatedBy("*Composable")
            }
        }
    }
}
