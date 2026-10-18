plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
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
    implementation(libs.material)
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

    implementation(libs.androidx.datastore.preferences)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.libphonenumber.android)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.mockk)
    testImplementation(libs.androidx.room.testing)
}

kover {
    currentProject {
        instrumentation {
            excludedClasses.addAll(
                "com.matheus.callmanager.BuildConfig",
                // "Hilt_*" is Hilt's generated superclass naming convention (e.g. Hilt_MainActivity) —
                // narrower than "*Hilt*" so it doesn't also swallow hand-written classes that happen
                // to have "Hilt" in their name, like HiltBroadcastReceiver.
                "com.matheus.callmanager.*Hilt_*",
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

/**
 * Classes expected to stay at (or near) full unit coverage: ViewModels and the pure rule/data
 * logic layer. Deliberately NOT the whole codebase — see CLAUDE.md's Testing section for classes
 * (raw ContentResolver call-log queries, contacts lookups, Composable screens) that are excluded
 * from this bar by design and are verified manually/on-device instead.
 */
val coreCoverageClasses =
    listOf(
        "com.matheus.callmanager.feature.recent.RecentViewModel",
        "com.matheus.callmanager.feature.lists.ListsViewModel",
        "com.matheus.callmanager.feature.rules.RulesViewModel",
        "com.matheus.callmanager.feature.debug.DebugViewModel",
        "com.matheus.callmanager.core.data.CallRepository",
        "com.matheus.callmanager.core.data.ScreeningPolicy",
        "com.matheus.callmanager.core.data.BlockedCallMerger",
        "com.matheus.callmanager.core.data.NameAllowMatcher",
        "com.matheus.callmanager.core.data.rule.BlockListRule",
        "com.matheus.callmanager.core.data.rule.AllowListRule",
        "com.matheus.callmanager.core.data.rule.NamePatternRule",
        "com.matheus.callmanager.core.data.rule.ContactRule",
        "com.matheus.callmanager.core.data.rule.RecentlyAcceptedRule",
        "com.matheus.callmanager.core.data.rule.RecentlyCalledRule",
        "com.matheus.callmanager.core.data.rule.UnknownNumberFallbackRule",
        "com.matheus.callmanager.core.datastore.DataStoreSettingsRepository",
        "com.matheus.callmanager.core.data.SystemBlockedCallsPager",
        "com.matheus.callmanager.core.di.DispatchersModule",
        "com.matheus.callmanager.core.di.DatabaseModule",
        "com.matheus.callmanager.core.di.HiltBroadcastReceiver",
    )

// Kover 0.8's verify{} rules can no longer filter by class (only a shared, report-wide filter is
// allowed), which would also hide these classes from the general HTML/XML reports. Reading the
// XML report directly keeps the coverage bar scoped to just this list.
tasks.register("verifyCoreCoverage") {
    group = "verification"
    description = "Fails if the ViewModel / rule-and-data-logic classes fall under 90% line coverage."
    dependsOn("koverXmlReportDebug")
    val reportFile = layout.buildDirectory.file("reports/kover/reportDebug.xml")
    inputs.file(reportFile)
    doLast {
        val classes =
            javax.xml.parsers.DocumentBuilderFactory
                .newInstance()
                .newDocumentBuilder()
                .parse(
                    reportFile.get().asFile,
                ).getElementsByTagName("class")
        val seen = mutableSetOf<String>()
        var covered = 0
        var missed = 0
        for (i in 0 until classes.length) {
            val classElement = classes.item(i) as org.w3c.dom.Element
            val name = classElement.getAttribute("name").replace('/', '.')
            if (name !in coreCoverageClasses) continue
            seen += name
            val children = classElement.childNodes
            for (j in 0 until children.length) {
                val child = children.item(j)
                if (child is org.w3c.dom.Element &&
                    child.tagName == "counter" &&
                    child.getAttribute("type") == "LINE"
                ) {
                    covered += child.getAttribute("covered").toInt()
                    missed += child.getAttribute("missed").toInt()
                }
            }
        }
        val notFound = coreCoverageClasses.toSet() - seen
        check(
            notFound.isEmpty(),
        ) { "Coverage-gated classes missing from the Kover report (renamed or moved?): $notFound" }
        val total = covered + missed
        val percentage = if (total == 0) 100.0 else 100.0 * covered / total
        val formatted = "%.1f".format(percentage)
        logger.lifecycle("Core logic coverage: $covered/$total lines ($formatted%)")
        check(percentage >= 90.0) {
            "Core logic coverage is $formatted%, below the required 90%."
        }
    }
}
