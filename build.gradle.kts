import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    java
    id("org.jetbrains.kotlin.jvm") version "2.4.10"
    application
    id("com.gradleup.shadow") version "9.6.1"
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://repo.blueva.net/releases")
}

dependencies {
    implementation("net.blueva:blueluak-jvm:26.2")
    implementation("net.blueva.foundation:BlueFoundation:26.32")
    implementation("com.google.code.gson:gson:2.10.1")

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

application {
    mainClass.set("net.blueva.arcade.cli.MainKt")
}

tasks.shadowJar {
    archiveBaseName.set("bacli")
    archiveClassifier.set("")
    archiveVersion.set(project.version.toString())
    // Avoids colliding with the plain `jar` task's own build/libs output when built standalone.
    // Not needed (and left alone) when included as a subproject of a larger build that redirects
    // shadowJar output itself.
    if (project == rootProject) {
        destinationDirectory.set(layout.buildDirectory.dir("dist"))
    }
    manifest {
        attributes["Main-Class"] = "net.blueva.arcade.cli.MainKt"
    }
}

tasks.test {
    useJUnitPlatform()
}
