import java.io.ByteArrayOutputStream
import org.gradle.jvm.tasks.Jar

plugins {
    kotlin("jvm") version "1.7.10"
    `maven-publish`
    application
}

group = "com.reevajs"
version = "1.0.0"
val release = false

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("com.ibm.icu:icu4j:71.1")
}

application {
    mainClass.set("com.reeva.regexp.AppKt")
}

tasks {
    test {
        useJUnitPlatform()
    }

    compileKotlin {
        kotlinOptions {
            jvmTarget = "1.8"
            freeCompilerArgs = listOf("-Xinline-classes", "-opt-in=kotlin.RequiresOptIn")
        }
    }

    compileTestKotlin {
        kotlinOptions {
            jvmTarget = "1.8"
        }
    }
}

val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = project.name
            version = if (release) project.version.toString() else getGitHash()

            from(components["java"])
            artifact(sourcesJar.get())
        }
    }
}

fun getGitHash(): String {
    val stdout = ByteArrayOutputStream()
    exec {
        commandLine("git", "rev-parse", "--short", "HEAD")
        standardOutput = stdout
    }
    return stdout.toString().trim()
}
