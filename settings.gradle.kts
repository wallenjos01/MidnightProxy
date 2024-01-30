pluginManagement {
    includeBuild("gradle/plugins")
}

rootProject.name = "midnightproxy"

include("api")
include("proxy")

include("plugin-test")