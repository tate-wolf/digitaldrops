plugins {
    id("java-library")
    id("com.gradleup.shadow") version "9.3.1"
}

group = "com.echochambers"
version = "1.0.0"
description = "Echo Drive Charger Plugin"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    compileOnly(files("libs/hytale-server.jar"))
    implementation("org.jetbrains:annotations:24.1.0")
    
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks {
    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release = 25
    }
    
    processResources {
        filteringCharset = Charsets.UTF_8.name()
        val props = mapOf(
            "group" to project.group,
            "version" to project.version,
            "description" to project.description
        )
        inputs.properties(props)
        filesMatching("manifest.json") {
            expand(props)
        }
    }
    
    shadowJar {
        archiveBaseName.set(rootProject.name)
        archiveClassifier.set("")
        minimize()
    }
    
    test {
        useJUnitPlatform()
    }
    
    build {
        dependsOn(shadowJar)
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

// danny - deploy to mods folder
tasks.register<Copy>("deploy") {
    dependsOn(tasks.shadowJar)
    
    val modsDir = file("${System.getProperty("user.home")}/AppData/Roaming/Hytale/UserData/Mods")
    
    from(tasks.shadowJar.get().archiveFile)
    into(modsDir)
    
    doFirst {
        println("Deploying ${tasks.shadowJar.get().archiveFile.get()} to $modsDir")
    }
    doLast {
        println("Deploy complete!")
    }
}
