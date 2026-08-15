import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.triplane.core.ai"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        val properties = Properties()
        val localProperties = project.rootProject.file("local.properties")
        if (localProperties.exists()) {
            properties.load(localProperties.inputStream())
        }
        val apiKey = properties.getProperty("GEMINI_API_KEY") ?: ""
        val pexelsApiKey = properties.getProperty("PEXELS_API_KEY") ?: ""
        
        buildConfigField("String", "GEMINI_API_KEY", "\"$apiKey\"")
        buildConfigField("String", "PEXELS_API_KEY", "\"$pexelsApiKey\"")
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(project(":core:auth"))
    implementation(project(":core:location"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.google.generativeai)
    implementation(libs.kotlinx.serialization.json)
    
    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.auth)
    
    // HTTP Client for API calls
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    
    testImplementation(libs.junit)
    testImplementation(libs.ktor.client.mock)
}
