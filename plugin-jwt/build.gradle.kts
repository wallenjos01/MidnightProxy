plugins {
    id("proxy-build")
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
    compileOnly(project(":api-jwt"))
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
