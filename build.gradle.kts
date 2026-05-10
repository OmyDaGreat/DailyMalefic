import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_25
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

buildscript {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }

    dependencies {
    }
}

kotlin {
    jvmToolchain(25)
}

application {
    mainClass = "xyz.malefic.daily.DailyMaleficKt"
}

repositories {
    mavenCentral()
    maven("https://maven.toastbits.dev")
}

tasks {
    withType<KotlinJvmCompile>().configureEach {
        compilerOptions {
            allWarningsAsErrors = false
            jvmTarget.set(JVM_25)
            freeCompilerArgs.add("-Xjvm-default=all")
        }
    }

    withType<Test> {
        useJUnitPlatform()
    }

    java {
    }
}

dependencies {
    implementation(platform(libs.http4k.bom))
    implementation(libs.bundles.http4k)
    implementation(libs.jackson.datatype.jsr310)

    implementation(libs.ytm.api) {
        // Avoid duplicate NewPipeExtractor-v0.24.5.jar entries during distZip.
        exclude(group = "com.github.teamnewpipe.NewPipeExtractor", module = "NewPipeExtractor")
    }

    testImplementation(libs.bundles.http4k.testing)
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}
