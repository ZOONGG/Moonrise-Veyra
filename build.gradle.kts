plugins {
    java
    id("net.weavemc.gradle") version "1.3.3"
}

group = "dev.veyra"
version = "0.1.1-moonrise.1"

base {
    archivesName.set("Veyra")
}

weave {
    configure {
        name = "Veyra"
        modId = "veyra"
        entryPoints = listOf("dev.veyra.weave.Main")
        mixinConfigs = listOf("veyra.mixins.json")
        accessWideners = listOf("veyra.accesswidener.txt")
        mcpMappings()
    }
    version("1.8.9")
}

repositories {
    maven("https://repo.spongepowered.org/maven/")
    maven("https://gitlab.com/api/v4/projects/80566527/packages/maven")
    mavenCentral()
}

dependencies {
    // Weave Gradle 1.3.3 only adds these after downloading Mojang's version
    // manifest. Keep the compile classpath available when that endpoint is
    // temporarily unreachable; all three are provided by Minecraft at runtime.
    compileOnly("org.lwjgl.lwjgl:lwjgl:2.9.4-nightly-20150209")
    compileOnly("com.mojang:authlib:1.5.21")
    compileOnly("com.mojang:netty:1.8.8")
    compileOnly("io.netty:netty-all:4.0.23.Final")
    compileOnly("org.projectlombok:lombok:1.18.46")
    annotationProcessor("org.projectlombok:lombok:1.18.46")
    compileOnly("net.weavemc:loader:1.3.4")
    compileOnly("net.weavemc.api:api:1.3.4")
    compileOnly("net.weavemc.api:api-v1_8:1.3.4")
    compileOnly("org.spongepowered:mixin:0.8.5")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("com.google.code.gson:gson:2.2.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.compileJava {
    options.release.set(17)
    options.encoding = "UTF-8"
}

tasks.test {
    useJUnitPlatform()
}

val moonriseHome = providers.gradleProperty("moonriseHome")
    .orElse(providers.environmentVariable("MOONRISE_HOME"))

tasks.register<Copy>("installToMoonrise") {
    group = "distribution"
    description = "Builds Veyra and copies it into Moonrise's Weave package directory."
    dependsOn(tasks.jar)
    from(tasks.jar)
    into(providers.provider {
        val root = moonriseHome.orNull
            ?: throw GradleException("Set -PmoonriseHome=<path> or MOONRISE_HOME before installing.")
        file(root).resolve("packages/weave")
    })
}
