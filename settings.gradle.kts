pluginManagement {
    includeBuild("gradle/plugins")
}

rootProject.name = "midnightproxy"

include("api")
include("api-jwt")
include("proxy")

include("plugin-test", "plugin-jwt", "plugin-whitelist", "plugin-lastserver", "plugin-sql")