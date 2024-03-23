pluginManagement {
    includeBuild("gradle/plugins")
}

rootProject.name = "midnightproxy"

include("api")
include("api-jwt")
include("proxy")

include("plugin-test")
include("plugin-jwt")
include("plugin-whitelist")
include("plugin-lastserver")