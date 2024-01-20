plugins {
    id("proxy-build")
    id("proxy-publish")
}

java.sourceCompatibility = JavaVersion.VERSION_17
java.targetCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
    maven("https://maven.wallentines.org/")
    mavenLocal()
}

dependencies {

    api("org.wallentines:midnightcfg-api:2.0.0-SNAPSHOT")
    api("org.wallentines:midnightcfg-codec-json:2.0.0-SNAPSHOT")
    api("org.wallentines:midnightlib:1.3.1-SNAPSHOT")

    api("org.wallentines:midnightcore-common:2.0.0-SNAPSHOT")

    api("io.netty:netty-codec:4.1.105.Final")
    api("io.netty:netty-buffer:4.1.105.Final")

    api("org.slf4j:slf4j-api:2.0.9")

    testImplementation(platform("org.junit:junit-bom:5.10.1"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
}

tasks.test {
    useJUnitPlatform()
}

