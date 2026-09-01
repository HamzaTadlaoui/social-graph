plugins {
    alias(libs.plugins.android.application)
    // AGP brings Kotlin itself; these two have to be applied at Kotlin's own
    // version, which is why the catalog pins kotlin, ksp and the compiler together.
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
}

android {
    namespace = "io.github.hamzatadlaoui.socialgraph"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "io.github.hamzatadlaoui.socialgraph"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// The generated schema is checked in, so a migration can be diffed against it.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    // The real org.json, so the backup format can be tested off-device -
    // the same trick :app uses for its page files.
    testImplementation(libs.json)
    androidTestImplementation(libs.androidx.junit)
}
