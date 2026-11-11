import com.vanniktech.maven.publish.AndroidSingleVariantLibrary

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.dokka)
}

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

mavenPublishing {
    // Publishes the release variant with a sources jar and a Dokka-generated javadoc jar.
    // Maven Central rejects a deployment that is missing either one.
    configure(
        AndroidSingleVariantLibrary(
            variant = "release",
            sourcesJar = true,
            publishJavadocJar = true
        )
    )

    coordinates(
        groupId = "io.github.amirezafahimi",
        artifactId = "cardscanner",
        version = "1.0.0"
    )

    // Uploads to the Central Portal and stops there: the deployment then waits in
    // https://central.sonatype.com/publishing/deployments for you to press Publish.
    // Pass automaticRelease = true once you trust the pipeline.
    publishToMavenCentral()

    // Every file needs a detached PGP signature. Keys come from gradle.properties or, in CI,
    // from the ORG_GRADLE_PROJECT_signingInMemoryKey environment variable. See PUBLISHING.md.
    signAllPublications()

    pom {
        name.set("CardScanner")
        description.set(
            "Jetpack Compose bank card scanner for Android: CameraX + ML Kit OCR with " +
                "Luhn and IIN validation, tuned for Iranian bank cards."
        )
        inceptionYear.set("2026")
        url.set("https://github.com/amirezafahimi/IranianBankCardScanner")

        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
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
            url.set("https://github.com/amirezafahimi/IranianBankCardScanner")
            connection.set("scm:git:git://github.com/amirezafahimi/IranianBankCardScanner.git")
            developerConnection.set("scm:git:ssh://git@github.com/amirezafahimi/IranianBankCardScanner.git")
        }
    }
}
