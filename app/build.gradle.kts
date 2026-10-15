plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

// Versionsnummern kommen aus dem Release-Workflow (-PversionCode / -PversionName).
val appVersionCode = (findProperty("versionCode") as String?)?.toIntOrNull() ?: 1
val appVersionName = (findProperty("versionName") as String?) ?: "0.0.0-dev"

// Fester Release-Schlüssel (signing/release.p12, passwortgeschützt).
// Passwort nur als GitHub-Secret KEYSTORE_PASSWORD. Ohne Passwort wird mit dem Debug-Schlüssel signiert
// (nur zum Testen; solche Builds lassen sich NICHT über die echte App installieren).
val keystorePassword: String? = System.getenv("KEYSTORE_PASSWORD")?.takeIf { it.isNotBlank() }

android {
    namespace = "de.kalorien.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "de.kalorien.app"
        minSdk = 30
        targetSdk = 35
        versionCode = appVersionCode
        versionName = appVersionName
    }

    signingConfigs {
        if (keystorePassword != null) {
            create("release") {
                storeFile = rootProject.file("signing/release.p12")
                storeType = "pkcs12"
                storePassword = keystorePassword
                keyAlias = "kalorien"
                keyPassword = keystorePassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = if (keystorePassword != null) signingConfigs.getByName("release")
            else signingConfigs.getByName("debug")
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
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
    sourceSets {
        // Room-Schemas für die Migrationstests (Robolectric liest die Assets der Debug-Variante;
        // in der Release-APK landen sie nicht)
        getByName("debug").assets.srcDir("$projectDir/schemas")
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

room {
    // Datenbank-Schema jeder Version wird hier abgelegt und eingecheckt (Grundlage für Migrationen)
    schemaDirectory("$projectDir/schemas")
}

ksp {
    arg("room.generateKotlin", "true")
}

dependencies {
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    // Datenbank-Migrationstests laufen ohne Gerät (Robolectric)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}

// Fehlgeschlagene Tests vollständig im Log zeigen (CI-Kommentar liest sie von dort)
tasks.withType<Test>().configureEach {
    testLogging {
        events("failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStackTraces = true
    }
}
