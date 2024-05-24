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

    api(libs.midnight.cfg)
    api(libs.midnight.cfg.json)
    api(libs.midnight.cfg.nbt)

    api(libs.midnight.lib)

    api(libs.slf4j.api)

    compileOnly(libs.jetbrains.annotations)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.slf4j.simple)
}

tasks.test {
    useJUnitPlatform()
}

