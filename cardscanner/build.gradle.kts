plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    `maven-publish`
}

group = "com.github.amirezafahimi"
version = "1.0.0"

android {
    namespace = "io.github.amirezafahimi.cardscanner"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        consumerProguardFiles("consumer-rules.pro")
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(libs.versions.jvmTarget.get())
        targetCompatibility = JavaVersion.toVersion(libs.versions.jvmTarget.get())
    }

    kotlinOptions {
        jvmTarget = libs.versions.jvmTarget.get()
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)

    implementation(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.foundation)
    api(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    api(libs.mlkit.text.recognition)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.github.amirezafahimi"
            artifactId = "cardscanner"
            version = project.version.toString()

            afterEvaluate {
                from(components["release"])
            }

            pom {
                name.set("CardScanner")
                description.set(
                    "Jetpack Compose bank card scanner for Android: CameraX + ML Kit OCR with " +
                        "Luhn and IIN validation, tuned for Iranian bank cards."
                )
                url.set("https://github.com/amirezafahimi/cardscanner")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://github.com/amirezafahimi/cardscanner/blob/main/LICENSE")
                    }
                }
                developers {
                    developer {
                        id.set("amirezafahimi")
                        name.set("Amirreza Fahimi")
                        url.set("https://github.com/amirezafahimi")
                    }
                }
                scm {
                    url.set("https://github.com/amirezafahimi/cardscanner")
                    connection.set("scm:git:git://github.com/amirezafahimi/cardscanner.git")
                    developerConnection.set("scm:git:ssh://git@github.com/amirezafahimi/cardscanner.git")
                }
            }
        }
    }
}
