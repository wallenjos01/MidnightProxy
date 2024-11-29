plugins {
    id("build.library")
}

dependencies {

    api(libs.midnight.cfg)
    api(libs.midnight.cfg.json)
    api(libs.midnight.cfg.nbt)

    api(libs.midnight.lib)

    api(libs.slf4j.api)

    compileOnlyApi(libs.jetbrains.annotations)

    testImplementation(libs.slf4j.simple)
}

tasks.test {
    useJUnitPlatform()
}

