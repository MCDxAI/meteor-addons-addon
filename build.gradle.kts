plugins {
    alias(libs.plugins.fabric.loom)
}

base {
    archivesName = properties["archives_base_name"] as String
    group = properties["maven_group"] as String
    version = libs.versions.mod.version.get()
}

repositories {
    maven {
        name = "meteor-maven"
        url = uri("https://maven.meteordev.org/releases")
    }
    maven {
        name = "meteor-maven-snapshots"
        url = uri("https://maven.meteordev.org/snapshots")
    }
    mavenCentral()
}

val modInclude: Configuration by configurations.creating

configurations {
    implementation.configure { extendsFrom(modInclude) }
    include.configure { extendsFrom(modInclude) }
}

dependencies {
    // Fabric
    minecraft(libs.minecraft)
    implementation(libs.fabric.loader)

    // Meteor
    implementation(libs.meteor.client)

    // HTTP client for downloading addons
    modInclude(libs.okhttp)
    include(libs.okio)
    include(libs.kotlin.stdlib)

    // JSON parsing
    modInclude(libs.gson)

    // Testing
    testImplementation(libs.junitApi)
    testImplementation(libs.junitParams)
    testRuntimeOnly(libs.junitEngine)
    testRuntimeOnly(libs.junitLauncher)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(libs.versions.jdk.get().toInt()))
    }
}

tasks {
    processResources {
        val propertyMap = mapOf(
            "version" to project.version,
            "mc_version" to libs.versions.minecraft.get()
        )

        inputs.properties(propertyMap)

        filteringCharset = "UTF-8"

        filesMatching("fabric.mod.json") {
            expand(propertyMap)
        }
    }

    jar {
        inputs.property("archivesName", project.base.archivesName.get())

        from("LICENSE") {
            rename { "${it}_${inputs.properties["archivesName"]}" }
        }
    }

    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release = 25
        options.compilerArgs.add("-Xlint:deprecation")
        options.compilerArgs.add("-Xlint:unchecked")
    }

    test {
        useJUnitPlatform()
    }
}
