plugins {
    id("proxy-build")
    id("proxy-publish")
}

java.sourceCompatibility = JavaVersion.VERSION_17
java.targetCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
    maven("https://maven.wallentines.org/")
    maven("https://libraries.minecraft.net/")
    mavenLocal()
}

dependencies {

    compileOnly(project(":api"))
}

tasks.test {
    useJUnitPlatform()
}


tasks.withType<ProcessResources>() {
    filesMatching("plugin.json") {
        expand(mapOf(
                Pair("version", project.version as String)
        ))
    }
}
