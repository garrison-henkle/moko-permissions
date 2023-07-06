/*
 * Copyright 2021 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("dev.icerock.moko.gradle.multiplatform.mobile")
    id("dev.icerock.moko.gradle.publication")
    id("dev.icerock.moko.gradle.stub.javadoc")
    id("dev.icerock.moko.gradle.detekt")
    `maven-publish`
    id("com.jfrog.artifactory")
}

val localProperties =
    org.jetbrains.kotlin.konan.properties.loadProperties(rootProject.file("local.properties").absolutePath)

android {
    namespace = "dev.icerock.moko.permissions.test"
}

dependencies {
    commonMainImplementation(libs.coroutines)

    androidMainImplementation(libs.appCompat)

    commonMainApi(libs.mokoPermissions)
}

kotlin {
    android {
        publishAllLibraryVariants()
    }

    ios()

    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> {
        binaries.framework(listOf(RELEASE))
    }
}

artifactory {
    val artifactoryUrl = localProperties.getProperty("artifactory.url")
    val artifactoryRepository = localProperties.getProperty("artifactory.repo")
    val artifactoryUsername = localProperties.getProperty("artifactory.username")
    val artifactoryPassword = localProperties.getProperty("artifactory.password")

    setContextUrl(artifactoryUrl)
    publish {
        repository {
            repoKey = artifactoryRepository
            username = artifactoryUsername
            password = artifactoryPassword
        }

        defaults {
            publications(org.jfrog.gradle.plugin.artifactory.Constant.ALL_PUBLICATIONS)
        }
    }
}
