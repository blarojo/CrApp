plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    jacoco
}

jacoco {
    toolVersion = "0.8.12"
}

android {
    namespace = "com.crapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.crapp"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            // Instruments debug bytecode for both test types below, so the
            // jacocoLogicLayerReport task (see bottom of this file) can measure real
            // coverage rather than an estimate.
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
    }

    sourceSets {
        // Room's MigrationTestHelper (see MigrationTest) reads historical schema JSON
        // (exported to app/schemas/ -- see the ksp { } block below) from assets.
        getByName("androidTest").assets.srcDirs("$projectDir/schemas")
    }
}

ksp {
    // Persists Room schema history to app/schemas/ (committed to the repo) so future
    // schema changes can be validated against prior versions for migrations.
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.work.runtime.ktx)
    // Wear OS companion app sync (docs/future-features.md spec 5, wear/ module) --
    // the Wearable Data Layer API this phone-side listener uses is only shipped as
    // part of Play Services, there's no way around this dependency for that feature.
    implementation(libs.play.services.wearable)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit)
    // Real org.json impl for JVM unit tests -- the Android SDK's own org.json classes
    // are stubs on this classpath (throw at runtime), since BackupSerializer uses
    // org.json (see its KDoc). Test-only; production code still uses Android's copy.
    testImplementation(libs.org.json)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.androidx.room.testing)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

// --- Coverage (docs/development-plan.md Phase 9) ------------------------------------
//
// "90% coverage" is scoped to the data/logic layer (data/**, export/**) -- Composable
// UI functions aren't meaningfully JVM-unit-testable without a heavy Robolectric setup,
// so the idiomatic Android split is: unit + instrumented tests drive the logic layer to
// a real number here, while the UI layer is covered separately by the Compose UI
// instrumented tests under app/src/androidTest/java/com/crapp/ui.
//
// Merges testDebugUnitTest + connectedDebugAndroidTest execution data, so run both
// before this task for an accurate report:
//   ./gradlew testDebugUnitTest connectedDebugAndroidTest jacocoLogicLayerReport
//
// !! connectedDebugAndroidTest / connectedCheck WILL UNINSTALL a real device's
// !! existing CrApp install first (applicationId com.crapp, shared with the debug
// !! build these tasks manage) -- this already happened once and wiped real logged
// !! data, recovered from a Settings -> Back Up Data backup taken minutes earlier.
// !! ALWAYS take a fresh backup on the device before running either task against a
// !! phone with a real install; the tests themselves are safe (they run against
// !! AppDatabase.useInMemoryDatabaseForTesting, never the real database) but the
// !! Gradle task's own install/uninstall lifecycle is not.
val jacocoFileFilter = listOf(
    "**/R.class", "**/R$*.class", "**/BuildConfig.*", "**/Manifest*.*",
    "android/**/*.*",
    "**/*_Impl.class", "**/*_Impl$*.class", // Room-generated
    "**/ui/**", // Composable UI + ViewModels: covered by androidTest/ui instead
    "**/MainActivity*.class", "**/CrAppApplication*.class", // thin composition roots
)

val jacocoDebugClasses = layout.buildDirectory.dir("tmp/kotlin-classes/debug").map { dir ->
    fileTree(dir) { exclude(jacocoFileFilter) }
}

tasks.register<JacocoReport>("jacocoLogicLayerReport") {
    group = "verification"
    description = "HTML/XML coverage report for the data/export layer (unit + instrumented)."
    dependsOn("testDebugUnitTest")

    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    sourceDirectories.setFrom(files("$projectDir/src/main/java"))
    classDirectories.setFrom(jacocoDebugClasses)
    executionData.setFrom(
        fileTree(layout.buildDirectory) {
            include(
                "outputs/unit_test_code_coverage/debugUnitTest/*.exec",
                "outputs/code_coverage/debugAndroidTest/connected/**/*.ec"
            )
        }
    )
}

tasks.register<JacocoCoverageVerification>("jacocoLogicLayerCoverageVerification") {
    group = "verification"
    description = "Fails if the data/export layer's line coverage drops below 90%."
    dependsOn("testDebugUnitTest")

    violationRules {
        rule {
            limit {
                counter = "LINE"
                minimum = "0.90".toBigDecimal()
            }
        }
    }

    sourceDirectories.setFrom(files("$projectDir/src/main/java"))
    classDirectories.setFrom(jacocoDebugClasses)
    executionData.setFrom(
        fileTree(layout.buildDirectory) {
            include(
                "outputs/unit_test_code_coverage/debugUnitTest/*.exec",
                "outputs/code_coverage/debugAndroidTest/connected/**/*.ec"
            )
        }
    )
}
