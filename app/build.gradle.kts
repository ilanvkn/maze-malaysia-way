plugins { id("com.android.application"); id("org.jetbrains.kotlin.android"); id("org.jetbrains.kotlin.plugin.compose") }
android {
 namespace = "my.maze.malaysia"
 compileSdk = 36
 buildToolsVersion = "35.0.0"
 defaultConfig { applicationId = "my.maze.malaysia"; minSdk = 26; targetSdk = 36; versionCode = 4; versionName = "0.3.1-offline-beta" }
 buildTypes {
 getByName("debug") { buildConfigField("boolean","IS_LAB","false"); buildConfigField("String","GATEWAY_BASE","\"\"") }
 getByName("release") { buildConfigField("boolean","IS_LAB","false"); val gateway=providers.gradleProperty("mazeGatewayBase").orNull ?: ""; require(gateway.isEmpty() || Regex("https://[a-zA-Z0-9.-]+(:[0-9]{1,5})?(/[a-zA-Z0-9_./-]*)?").matches(gateway)) { "mazeGatewayBase must be a credential-free HTTPS base URL" }; buildConfigField("String","GATEWAY_BASE","\""+gateway+"\""); isMinifyEnabled = true; isShrinkResources = true; proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro") }
 create("lab") { initWith(getByName("release")); signingConfig=signingConfigs.getByName("debug"); applicationIdSuffix=".lab"; versionNameSuffix="-lab"; buildConfigField("boolean","IS_LAB","true"); buildConfigField("String","GATEWAY_BASE","\"http://10.0.2.2:8787\"") }
 }
 buildFeatures { compose = true; buildConfig = true }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget = "17" }
}
dependencies {
 implementation(platform("androidx.compose:compose-bom:2025.04.01"))
 implementation("androidx.activity:activity-compose:1.10.1")
 implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
 implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
 implementation("androidx.lifecycle:lifecycle-viewmodel-savedstate:2.8.7")
 implementation("androidx.compose.ui:ui")
 implementation("androidx.compose.foundation:foundation")
 implementation("androidx.compose.material3:material3")
 implementation("org.maplibre.gl:android-sdk-opengl:13.7.1")
 implementation("com.squareup.okhttp3:okhttp:4.12.0")
 testImplementation("junit:junit:4.13.2")
 testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
 testImplementation("org.json:json:20240303")
}
