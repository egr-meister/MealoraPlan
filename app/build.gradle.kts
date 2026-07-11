import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
}

/**
 * Reads release signing configuration from (in priority order):
 *   1. Environment variables (used by CI / GitHub Actions).
 *   2. A local, git-ignored keystore.properties file (used by developers).
 *
 * If no complete configuration is found, the release signing config is simply
 * not created and a clear error is raised only when a signed release build is
 * actually requested. Debug builds always work with the standard debug key.
 */
data class ReleaseSigning(
    val storeFilePath: String,
    val storePassword: String,
    val keyAlias: String,
    val keyPassword: String,
)

// Computed inline (not as a top-level function) so that `rootProject` — an
// implicit Project receiver — is accessible.
val releaseSigning: ReleaseSigning? = run {
    // 1. Environment variables (CI).
    val envStore = System.getenv("ANDROID_KEYSTORE_FILE")
    val envStorePass = System.getenv("ANDROID_KEYSTORE_PASSWORD")
    val envAlias = System.getenv("ANDROID_KEY_ALIAS")
    val envKeyPass = System.getenv("ANDROID_KEY_PASSWORD")
    if (!envStore.isNullOrBlank() && !envStorePass.isNullOrBlank() &&
        !envAlias.isNullOrBlank() && !envKeyPass.isNullOrBlank()
    ) {
        ReleaseSigning(envStore, envStorePass, envAlias, envKeyPass)
    } else {
        // 2. Local keystore.properties (never committed).
        val propsFile = rootProject.file("keystore.properties")
        if (propsFile.exists()) {
            val props = Properties().apply { load(FileInputStream(propsFile)) }
            val storeFile = props.getProperty("storeFile")
            val storePassword = props.getProperty("storePassword")
            val keyAlias = props.getProperty("keyAlias")
            val keyPassword = props.getProperty("keyPassword")
            if (!storeFile.isNullOrBlank() && !storePassword.isNullOrBlank() &&
                !keyAlias.isNullOrBlank() && !keyPassword.isNullOrBlank()
            ) {
                ReleaseSigning(storeFile, storePassword, keyAlias, keyPassword)
            } else {
                null
            }
        } else {
            null
        }
    }
}

android {
    namespace = "com.mealora.plan"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mealora.plan"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        if (releaseSigning != null) {
            create("release") {
                storeFile = file(releaseSigning.storeFilePath)
                storePassword = releaseSigning.storePassword
                keyAlias = releaseSigning.keyAlias
                keyPassword = releaseSigning.keyPassword
                enableV1Signing = true
                enableV2Signing = true
            }
        }
    }

    buildTypes {
        getByName("release") {
            // Stage 1 (current): a non-minified release, validated first for
            // stability. After the non-minified release build has been installed,
            // launched and fully tested, flip BOTH flags to true to enable R8 and
            // resource shrinking (see README "Release Stability"). The ProGuard
            // rules required for Kotlinx Serialization are already provided.
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )

            // Use the real release signing config when credentials are available.
            // When they are not, no signingConfig is attached here and a clear
            // failure is raised at execution time by the top-level guard below —
            // the debug key is never used for release artifacts.
            if (releaseSigning != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        getByName("debug") {
            isMinifyEnabled = false
            isShrinkResources = false
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
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

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.core.splashscreen)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    coreLibraryDesugaring(libs.desugar.jdk.libs)

    debugImplementation(libs.androidx.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

// Fail clearly (never silently fall back to the debug key) if a signed release
// is requested without signing credentials. Evaluated at execution time.
gradle.taskGraph.whenReady {
    if (releaseSigning == null) {
        val buildingRelease = allTasks.any {
            val n = it.name.lowercase()
            n.contains("assemblerelease") || n.contains("bundlerelease")
        }
        if (buildingRelease) {
            throw GradleException(
                "Release signing credentials are missing. Provide the " +
                    "ANDROID_KEYSTORE_FILE / ANDROID_KEYSTORE_PASSWORD / ANDROID_KEY_ALIAS / " +
                    "ANDROID_KEY_PASSWORD environment variables, or a keystore.properties file at " +
                    "the project root. Refusing to build a release without a real keystore.",
            )
        }
    }
}
