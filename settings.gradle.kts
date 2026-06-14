pluginManagement {
    repositories {
        gradlePluginPortal() // Gradle 插件中心通常放在最前面
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        maven(url = "https://jitpack.io") // 添加 JitPack 仓库
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io") // 添加 JitPack 仓库
    }
}

rootProject.name = "工具箱"
include(":app")