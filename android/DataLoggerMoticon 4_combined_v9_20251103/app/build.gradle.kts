plugins {
    id("com.android.application")
}

android {
    namespace = "com.example.dataloggerrev001"
    compileSdk = 33

    defaultConfig {
        applicationId = "com.example.dataloggerrev001"
        minSdk = 28
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    }

    buildTypes {
        create("customDebugType"){
            isDebuggable  = true
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }


}

dependencies {
    implementation ("org.jetbrains.kotlin:kotlin-stdlib:1.8.0")
    implementation ("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.8.0")
    implementation ("androidx.navigation:navigation-fragment:2.4.2")
    implementation ("androidx.navigation:navigation-ui:2.4.2")

    implementation (files("libs/moticon_insole3_service-03_05_00-release.aar"))
    implementation ("com.google.protobuf:protobuf-javalite:3.21.11")
    implementation ("no.nordicsemi.android.support.v18:scanner:1.6.0")
    implementation ("no.nordicsemi.android:dfu:2.2.2")
    implementation ("no.nordicsemi.android:ble:2.6.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.8.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    implementation ("com.jjoe64:graphview:4.2.2")
    implementation ("androidx.work:work-runtime:2.7.1")
    implementation ("androidx.lifecycle:lifecycle-runtime:2.3.1")
    implementation (files("libs/MovellaDotSdkCore_ANDROID_v2023.6.0_STABLE_RELEASE.aar"))
    implementation (files("libs/MovellaDotSdkMfm_ANDROID_v2023.6.0_STABLE_RELEASE.aar"))
    implementation (files("libs/MovellaDotSdkOta_ANDROID_v2023.6.0_STABLE_RELEASE.aar"))




}