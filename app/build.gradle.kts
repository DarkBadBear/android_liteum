import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.gms.services) // 여기서는 버전 명시 안 함 (프로젝트 수준에서 관리)

    kotlin("kapt")
}

hilt {
    enableAggregatingTask = false
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.reader().use { reader ->
        localProperties.load(reader)
    }
}

android {
    namespace = "com.peachspot.legendkofarm"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.peachspot.legendkofarm"
        minSdk = 32
        targetSdk = 35
        versionCode = 20
        versionName = "1.3.7"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["MAPS_API_KEY"] =
            localProperties.getProperty("MAPS_API_KEY", "")
    }

    signingConfigs {
        create("shared") {
            storeFile = file(localProperties.getProperty("storeFile") as String)
            storePassword = localProperties.getProperty("storePassword") as String
            keyAlias = localProperties.getProperty("keyAlias") as String
            keyPassword = localProperties.getProperty("keyPassword") as String
        }
    }

    buildTypes {
        debug {

            signingConfig = signingConfigs.getByName("shared")
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

        }

        release {

            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("shared")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

        }
    }
//이거 말고 다른 크래쉬 없나?
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }
}

dependencies {

    implementation(libs.google.tink)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kakao.all)
    implementation(libs.kakao.user)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.security.crypto.ktx)


    implementation(platform(libs.firebase.bom))
    implementation(libs.androidx.swipeRefreshLayout) // Use the alias you defined in libs.versions.toml


    implementation(libs.firebase.appcheck)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.appcheck.playintegrity)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.common)
    implementation(libs.firebase.config)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.messaging)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.material3)

    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.javax.inject)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.datastore.core.android)

    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.health.services.client)

    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.playServicesAuth)
    implementation(libs.googleid)

    implementation(libs.maps.compose)
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)

    implementation(libs.accompanist.permissions)

    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.kotlinxCoroutinesCore) // kotlinx-coroutines-core의 alias로 가정

    implementation(libs.androidx.material.icons)

    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.gson)

    implementation(libs.room)
    implementation(libs.roomKtx)
    kapt(libs.roomCompiler)

    implementation(libs.gson)

    implementation(libs.coil.compose)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom)) // 중복될 수 있으나, compose test bom이 별도로 있다면 유지
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)


    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}