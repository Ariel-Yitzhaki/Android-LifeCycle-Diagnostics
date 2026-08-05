plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    `maven-publish`
}

android {
    namespace = "com.ariel.diagnostics"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        minSdk = 29

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }

    // AGP 8 publishes nothing unless the variant is named here. Sources go out with the AAR so
    // consumers can step into the library and read the comments explaining each measurement.
    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    // Needed for FragmentManager.FragmentLifecycleCallbacks, which is how fragment timings are measured.
    implementation(libs.androidx.fragment.ktx)
    // Needed for JankStats, which is how dropped frames are counted per screen in Feature 4.
    implementation(libs.androidx.metrics.performance)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

// Wrapped in afterEvaluate because the Android build components this reads are not created until
// evaluation has finished.
afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])

                // groupId, artifactId and version are deliberately left at their defaults. JitPack
                // runs the build with -Pgroup and -Pversion, so the coordinate follows the tag
                // being built rather than a number hardcoded here.

                pom {
                    name.set("Android Lifecycle Diagnostics")
                    description.set(
                        "Runtime diagnostics for Android lifecycles: callback timing, callback " +
                            "order validation, retained screen detection and main-thread blocking, " +
                            "all reported to Logcat in plain English.",
                    )
                    url.set("https://github.com/Ariel-Yitzhaki/Android-LifeCycle-Diagnostics")

                    licenses {
                        license {
                            name.set("MIT License")
                            url.set(
                                "https://github.com/Ariel-Yitzhaki/Android-LifeCycle-Diagnostics/blob/main/LICENSE",
                            )
                        }
                    }

                    developers {
                        developer {
                            id.set("Ariel-Yitzhaki")
                            name.set("Ariel Yitzhaki")
                        }
                    }

                    scm {
                        url.set("https://github.com/Ariel-Yitzhaki/Android-LifeCycle-Diagnostics")
                        connection.set(
                            "scm:git:https://github.com/Ariel-Yitzhaki/Android-LifeCycle-Diagnostics.git",
                        )
                        developerConnection.set(
                            "scm:git:ssh://git@github.com/Ariel-Yitzhaki/Android-LifeCycle-Diagnostics.git",
                        )
                    }
                }
            }
        }
    }
}
