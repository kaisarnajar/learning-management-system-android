import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.detekt)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

/** Google OAuth *web* client id (the server's AUTH_GOOGLE_ID). Empty hides the Google button. */
val googleWebClientId: String = localProperties.getProperty("GOOGLE_WEB_CLIENT_ID")
    ?: System.getenv("GOOGLE_WEB_CLIENT_ID")
    ?: ""

/** A build setting from local.properties, or else from the environment (CI). */
fun setting(name: String): String? = localProperties.getProperty(name) ?: System.getenv(name)

/** The upload key that signs release builds. Never committed: see README "Release builds". */
val releaseSigning = listOf("RELEASE_STORE_FILE", "RELEASE_STORE_PASSWORD", "RELEASE_KEY_ALIAS", "RELEASE_KEY_PASSWORD")
    .associateWith { setting(it) }
val hasReleaseSigning = releaseSigning.values.all { !it.isNullOrBlank() }

/** The staging copy of the website, which the staging flavor talks to. */
val stagingBaseUrl: String = setting("STAGING_BASE_URL") ?: "https://staging-learning-management-system-teal-ten.vercel.app"

/**
 * The version code of this build: `-PversionCode=…` (a release pipeline passes an increasing number), else the last
 * one set here. Must only ever go up for a published app.
 */
val appVersionCode: Int = (findProperty("versionCode") as String?)?.toIntOrNull() ?: 2

android {
    namespace = "com.darsequran.academy"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.darsequran.academy"
        minSdk = 26
        targetSdk = 37
        versionCode = appVersionCode
        versionName = "2.0.0"
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Which copy of the website the app talks to, fixed when it is built: there is no switch inside the app.
    // staging: testing, never real data. production: the published app.
    flavorDimensions += "server"
    productFlavors {
        create("staging") {
            dimension = "server"
            applicationIdSuffix = ".staging"
            buildConfigField("String", "SERVER", "\"staging\"")
            buildConfigField("String", "BASE_URL", "\"$stagingBaseUrl\"")
        }
        create("production") {
            dimension = "server"
            buildConfigField("String", "SERVER", "\"production\"")
            buildConfigField("String", "BASE_URL", "\"https://darsequranacademy.com\"")
        }
    }

    signingConfigs {
        if (hasReleaseSigning) create("release") {
            storeFile = rootProject.file(releaseSigning.getValue("RELEASE_STORE_FILE")!!)
            storePassword = releaseSigning.getValue("RELEASE_STORE_PASSWORD")
            keyAlias = releaseSigning.getValue("RELEASE_KEY_ALIAS")
            keyPassword = releaseSigning.getValue("RELEASE_KEY_PASSWORD")
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Signed per flavor below: stagingRelease with the debug key (anyone can install and test the shrunk app),
            // productionRelease only with the upload key (checkReleaseSigning stops it without one).
            signingConfig = null
        }
    }
    productFlavors.getByName("staging").signingConfig = signingConfigs.getByName("debug")
    productFlavors.getByName("production").signingConfig =
        if (hasReleaseSigning) signingConfigs.getByName("release") else null

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
    }

    testOptions {
        // JVM tests: android.* calls such as Log.w return defaults instead of throwing.
        unitTests.isReturnDefaultValues = true
        // Robolectric tests (Context, DataStore, Compose UI) run on the JVM, here and in CI.
        unitTests.isIncludeAndroidResources = true
    }

    lint {
        // Today's warnings are frozen in the baseline; anything new fails the build.
        baseline = file("lint-baseline.xml")
        abortOnError = true
        warningsAsErrors = true
        // "A newer version exists" changes with every library release, not with this code.
        disable += setOf("NewerVersionAvailable", "GradleDependency", "AndroidGradlePluginVersion")
    }

    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.exifinterface)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.richeditor.compose)
    // Draws the UPI QR code the website shows (encoder only, no camera or Android code).
    implementation(libs.zxing.core)

    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.google.id)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

val checkReleaseSigning by tasks.registering {
    val configured = hasReleaseSigning
    doLast {
        if (!configured) throw GradleException(
            "Release signing is not configured. Set RELEASE_STORE_FILE, RELEASE_STORE_PASSWORD, RELEASE_KEY_ALIAS and " +
                "RELEASE_KEY_PASSWORD in local.properties or the environment (see README, \"Release builds\").",
        )
    }
}
tasks.matching { it.name == "packageProductionRelease" || it.name == "bundleProductionRelease" }
    .configureEach { dependsOn(checkReleaseSigning) }

// The launcher name says which app it is: four can sit side by side on one phone.
androidComponents {
    onVariants { variant ->
        val server = variant.productFlavors.first { it.first == "server" }.second
        val name = when {
            server == "production" && variant.buildType == "release" -> "Darse Quran Academy"
            server == "production" -> "DQA (debug)"
            variant.buildType == "release" -> "DQA Staging"
            else -> "DQA Staging (debug)"
        }
        variant.resValues.put(variant.makeResValueKey("string", "app_name"), com.android.build.api.variant.ResValue(name))
    }
}

kotlin {
    compilerOptions {
        optIn.addAll(
            "androidx.compose.material3.ExperimentalMaterial3Api",
            "androidx.compose.foundation.layout.ExperimentalLayoutApi",
        )
    }
}

detekt {
    // Today's findings are frozen in the baseline; new ones fail the build.
    buildUponDefaultConfig = true
    config.setFrom(file("detekt.yml"))
    baseline = file("detekt-baseline.xml")
}
