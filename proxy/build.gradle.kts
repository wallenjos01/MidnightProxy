plugins {
    id("proxy-build")
    id("application")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

application.mainClass.set("org.wallentines.mdproxy.Main")

java.sourceCompatibility = JavaVersion.VERSION_17
java.targetCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
    maven("https://maven.wallentines.org/")
    maven("https://libraries.minecraft.net/")
    mavenLocal()
}

configurations.shadow {
    extendsFrom(configurations.implementation.get())
}

dependencies {

    implementation(project(":api"))

    implementation("org.wallentines:midnightcfg-api:2.0.0-SNAPSHOT")
    implementation("org.wallentines:midnightcfg-codec-json:2.0.0-SNAPSHOT")
    implementation("org.wallentines:midnightlib:1.5.0-SNAPSHOT")

    implementation("org.wallentines:midnightcore-common:2.0.0-SNAPSHOT")

    implementation("io.netty:netty-all:4.1.105.Final")

    implementation("org.slf4j:slf4j-api:2.0.9")
    implementation("org.slf4j:slf4j-simple:2.0.9")

    implementation("com.mojang:authlib:6.0.52")
    implementation("com.google.guava:guava:33.0.0-jre")

    testImplementation(platform("org.junit:junit-bom:5.9.2"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
}

tasks.test {
    useJUnitPlatform()
}

java {
    manifest {
        attributes(Pair("Main-Class", application.mainClass))
    }
}

tasks.withType<JavaExec> {

    workingDir = File("run")
}
