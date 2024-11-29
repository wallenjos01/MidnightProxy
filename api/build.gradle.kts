plugins {
    id("build.library")
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

    compileOnlyApi(libs.jetbrains.annotations)
}
