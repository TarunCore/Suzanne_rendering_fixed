plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {
    namespace = "com.example.suzanne"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.suzanne"
        minSdk = 24
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
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
    aaptOptions {
        noCompress += "filamat"
    }
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
    //implementation(libs.androidx.room.compiler.processing.testing)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

//    implementation("androidx.core:core-ktx:1.12.0") // Latest version
//    implementation("androidx.appcompat:appcompat:1.6.1") // Latest stable
//    implementation("com.google.android.material:material:1.10.0") // Latest version
//    implementation("androidx.constraintlayout:constraintlayout:2.1.4") // Latest stable
//    testImplementation("junit:junit:4.13.2") // Latest stable
//    androidTestImplementation("androidx.test.ext:junit:1.1.5") // Latest stable
//    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.0") // Latest version

    implementation("com.google.android.filament:filament-android:1.42.1")
    implementation("com.google.android.filament:filament-utils-android:1.42.1")
}



//android {
//    namespace = "com.example.suzanne"
//    compileSdk = 34
//
//    defaultConfig {
//        applicationId = "com.example.suzanne"
//        minSdk = 24
//        targetSdk = 33
//        versionCode = 1
//        versionName = "1.0"
//
//        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
//        vectorDrawables {
//            useSupportLibrary = true
//        }
//    }
//
//    buildTypes {
//        release {
//            isMinifyEnabled = false
//            proguardFiles(
//                getDefaultProguardFile("proguard-android-optimize.txt"),
//                "proguard-rules.pro"
//            )
//        }
//    }
//
//
//
//    kotlinOptions {
//        jvmTarget = "1.8"
//    }
//
//    androidResources {
//        noCompress("filamat", "ktx")
//    }
//
//    buildFeatures {
//        compose = true
//    }
//    composeOptions {
//        kotlinCompilerExtensionVersion = "1.5.1"
//    }
//    packaging {
//        resources {
//            excludes += "/META-INF/{AL2.0,LGPL2.1}"
//        }
//    }
//}
//
//
//
//dependencies {
//    implementation(libs.androidx.core.ktx)
//    implementation(libs.androidx.lifecycle.runtime.ktx)
//    implementation(libs.androidx.activity.compose)
//    implementation(platform(libs.androidx.compose.bom))
//    implementation(libs.androidx.ui)
//    implementation(libs.androidx.ui.graphics)
//    implementation(libs.androidx.ui.tooling.preview)
//    implementation(libs.androidx.material3)
//    // implementation(libs.androidx.room.compiler.processing.testing)
//    testImplementation(libs.junit)
//    androidTestImplementation(libs.androidx.junit)
//    androidTestImplementation(libs.androidx.espresso.core)
//    androidTestImplementation(platform(libs.androidx.compose.bom))
//    androidTestImplementation(libs.androidx.ui.test.junit4)
//    debugImplementation(libs.androidx.ui.tooling)
//    debugImplementation(libs.androidx.ui.test.manifest)
//
//    implementation("com.google.android.filament:filament-android:1.42.1")
//    implementation("com.google.android.filament:filament-utils-android:1.42.1")
//    implementation ("deps.kotlin")
//
//}



//plugins {
//    alias(libs.plugins.android.application)
//    alias(libs.plugins.jetbrains.kotlin.android)
//    id("filament-tools-plugin")
//
//
//
//}
//
////project.ext.isSample = true
////
////kotlin {
////    jvmToolchain(versions.jdk)
////}
//
//filamentTools {
//    materialInputDir.set(project.layout.projectDirectory.dir("src/main/materials"))
//    materialOutputDir.set(project.layout.projectDirectory.dir("src/main/assets/materials"))
//}
//
//
//
//android {
//    namespace = "com.example.suzanne"
//    compileSdk = 34
//
//    defaultConfig {
//        applicationId = "com.example.suzanne"
//        minSdk = 24
//        targetSdk = 33
//        versionCode = 1
//        versionName = "1.0"
//
//        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
//        vectorDrawables {
//            useSupportLibrary = true
//        }
//    }
//
//    buildTypes {
//        release {
//            isMinifyEnabled = false
//            proguardFiles(
//                getDefaultProguardFile("proguard-android-optimize.txt"),
//                "proguard-rules.pro"
//            )
//        }
//    }
//
//    tasks.register("customClean") {
//        doLast {
//            println("Running custom clean task")
//        }
//}
//
//    compileOptions {
//        sourceCompatibility = JavaVersion.VERSION_1_8
//        targetCompatibility = JavaVersion.VERSION_1_8
//    }
//
//    kotlinOptions {
//        jvmTarget = "1.8"
//    }
//
//    buildFeatures {
//        compose = true
//    }
//
//    composeOptions {
//        kotlinCompilerExtensionVersion = "1.5.1"
//    }
//
//    packaging {
//        resources {
//            excludes += "/META-INF/{AL2.0,LGPL2.1}"
//        }
//    }
//
//    // Resources specific to filament
//    androidResources {
//        noCompress("filamat", "ktx")
//    }
//}
//
//dependencies {
//    implementation(libs.androidx.core.ktx)
//    implementation(libs.androidx.lifecycle.runtime.ktx)
//    implementation(libs.androidx.activity.compose)
//    implementation(platform(libs.androidx.compose.bom))
//    implementation(libs.androidx.ui)
//    implementation(libs.androidx.ui.graphics)
//    implementation(libs.androidx.ui.tooling.preview)
//    implementation(libs.androidx.material3)
//    testImplementation(libs.junit)
//    androidTestImplementation(libs.androidx.junit)
//    androidTestImplementation(libs.androidx.espresso.core)
//    androidTestImplementation(platform(libs.androidx.compose.bom))
//    androidTestImplementation(libs.androidx.ui.test.junit4)
//    debugImplementation(libs.androidx.ui.tooling)
//    debugImplementation(libs.androidx.ui.test.manifest)
//
//    // Filament dependencies
//    implementation("com.google.android.filament:filament-android:1.56.0")
//    implementation("com.google.android.filament:filament-utils-android:1.56.0")
//
//    //implementation(deps.kotlin)
//    implementation(project(":filament-android"))
//}