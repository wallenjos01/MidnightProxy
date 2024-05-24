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

    api(libs.midnight.core)

    api(libs.netty.codec)
    api(libs.netty.buffer)

    api(libs.google.guava)
    api(libs.slf4j.api)

    compileOnly(libs.jetbrains.annotations)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
}

tasks.test {
    useJUnitPlatform()
}

