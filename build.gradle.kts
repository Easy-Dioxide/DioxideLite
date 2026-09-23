// ---------------------------------------------------------------------------
// DioxideLite —— 由 SetsunaClient 的构建脚本改写而来。
//
// 与上游的差异（DioxideLite 是单 mod 结构）：
//   1. 上游把 SetsunaVia 作为 src/vendored/setsunavia 的独立源码集并产出多个
//      mod（setsunavia / setsunavia-api / setsunavia-visuals）。DioxideLite 把
//      协议翻译层直接内联在 com/viaversion/setsunavia/** 下，fabric.mod.json
//      也只有一个 mod，因此这里不需要额外的 srcDirs 与 processResources 合并。
//   2. 上游的 nested 库（67 个）通过 flatDir libs/nested 引入。若你的工程里没有
//      libs/nested 目录，这一段会自动跳过，不影响编译。
//   3. 26.1 起 Loom 插件坐标改为 net.fabricmc.fabric-loom，且不再需要 mappings 行。
// ---------------------------------------------------------------------------

plugins {
    id("net.fabricmc.fabric-loom") version "1.15.5"
}

val modId = project.property("mod_id").toString()
val modName = project.property("mod_name").toString()
val modAuthor = project.property("mod_author").toString()
val javaVersion = project.property("java_version").toString()
val minecraftVersion = project.property("minecraft_version").toString()

val nestedLibDir = layout.projectDirectory.dir("libs/nested").asFile
val hasNestedLibs = nestedLibDir.isDirectory
val nestedLibraryModules = if (hasNestedLibs) (1..67).map { "nested-%03d".format(it) } else emptyList()

val modMenuLocalJar = file(providers.gradleProperty("modmenu_jar")
        .orElse("libs/modmenu-18.0.0-alpha.8.jar")
        .get())

val supportedWebRtcPlatforms = setOf(
        "windows-x86_64", "windows-aarch64", "linux-x86_64", "linux-aarch64", "macos-aarch64"
)
val webRtcPlatform = providers.gradleProperty("webrtc_platform")
        .orElse("windows-x86_64")
        .get()
        .also { require(it in supportedWebRtcPlatforms) }

base {
    archivesName.set(modName)
}

version = project.property("version").toString()
group = project.property("group").toString()

repositories {
    if (hasNestedLibs) flatDir { dirs("libs/nested") }
    mavenCentral()
    maven("https://repo.viaversion.com")
    maven("https://maven.lenni0451.net/everything")
    maven("https://maven.terraformersmc.com/releases")
    maven("https://jitpack.io") {
        content {
            includeGroup("com.github.oryxel1")
            includeGroup("com.github.FPSMasterTeam")
        }
    }
    exclusiveContent {
        forRepository {
            maven {
                name = "Sponge"
                url = uri("https://repo.spongepowered.org/repository/maven-public")
            }
        }
        filter { includeGroupAndSubgroups("org.spongepowered") }
    }
    exclusiveContent {
        forRepository {
            maven {
                name = "CaffeineMC"
                url = uri("https://maven.caffeinemc.net/releases")
            }
        }
        filter { includeGroup("net.caffeinemc") }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${minecraftVersion}")
    implementation("net.fabricmc:fabric-loader:${project.property("fabric_loader_version")}")
    implementation("net.fabricmc.fabric-api:fabric-api:${project.property("fabric_version")}")

    // 移植 Xray 的 Sodium / Indigo 兼容路径所需
    compileOnly("net.caffeinemc:sodium-fabric:0.8.12+mc26.1.2")

    // 内嵌音乐子系统（tritium / Cadence）与部分协议代码使用 lombok
    compileOnly("org.projectlombok:lombok:1.18.42")
    annotationProcessor("org.projectlombok:lombok:1.18.42")

    // 运行时依赖：implementation 编译时 + include 打包进 jar
    implementation("com.google.zxing:core:3.5.1")
    include("com.google.zxing:core:3.5.1")
    implementation("org.luaj:luaj-jse:3.0.1")
    include("org.luaj:luaj-jse:3.0.1")
    implementation("com.github.FPSMasterTeam:Cadence:v0.1.1") {
        exclude(group = "com.google.code.gson", module = "gson")
    }
    include("com.github.FPSMasterTeam:Cadence:v0.1.1") {
        exclude(group = "com.google.code.gson", module = "gson")
    }
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.4.0")
    include("org.jetbrains.kotlin:kotlin-stdlib:2.4.0")

    // mixin 相关：Xray 的 @WrapOperation 与 FabricMixinPlugin 的 ASM 依赖
    compileOnly("io.github.llamalad7:mixinextras-common:0.5.3")
    compileOnly("org.ow2.asm:asm:9.8")
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
    compileOnly("org.checkerframework:checker-qual:3.12.0")

    if (modMenuLocalJar.isFile) compileOnly(files(modMenuLocalJar))
    else compileOnly("com.terraformersmc:modmenu:18.0.0-alpha.8")
    val annotationsJar = file("libs/annotations.jar")
    if (annotationsJar.isFile) compileOnly(files(annotationsJar))

    testImplementation("org.junit.jupiter:junit-jupiter:5.12.2")
    testImplementation("org.ow2.asm:asm:9.9")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.12.2")

    // ---- 内联的协议翻译层（com.viaversion.dioxidelitevia）运行时依赖 ----
    implementation("com.viaversion:viaversion-common:5.10.0")
    include("com.viaversion:viaversion-common:5.10.0")
    implementation("com.viaversion:viabackwards-common:5.10.0")
    include("com.viaversion:viabackwards-common:5.10.0")
    implementation("com.viaversion:viaaprilfools-common:4.2.1")
    include("com.viaversion:viaaprilfools-common:4.2.1")
    implementation("net.raphimc:ViaLegacy:3.0.16")
    include("net.raphimc:ViaLegacy:3.0.16")
    implementation("com.seedfinding:mc_biome:1.171.1")
    include("com.seedfinding:mc_biome:1.171.1")
    implementation("com.seedfinding:mc_noise:1.171.1")
    include("com.seedfinding:mc_noise:1.171.1")
    implementation("com.seedfinding:mc_seed:1.171.2")
    include("com.seedfinding:mc_seed:1.171.2")
    implementation("com.seedfinding:mc_math:1.171.0")
    include("com.seedfinding:mc_math:1.171.0")
    implementation("com.seedfinding:mc_core:1.210.0")
    include("com.seedfinding:mc_core:1.210.0")
    implementation("net.raphimc:ViaBedrock:0.0.29-SNAPSHOT") {
        exclude(group = "com.mojang", module = "brigadier")
        exclude(group = "at.yawk.lz4", module = "lz4-java")
        exclude(group = "io.netty")
    }
    include("net.raphimc:ViaBedrock:0.0.29-SNAPSHOT") {
        exclude(group = "com.mojang", module = "brigadier")
        exclude(group = "at.yawk.lz4", module = "lz4-java")
        exclude(group = "io.netty")
    }
    implementation("io.jsonwebtoken:jjwt-api:0.13.0")
    include("io.jsonwebtoken:jjwt-api:0.13.0")
    implementation("io.jsonwebtoken:jjwt-impl:0.13.0")
    include("io.jsonwebtoken:jjwt-impl:0.13.0")
    implementation("io.jsonwebtoken:jjwt-gson:0.13.0") {
        exclude(group = "com.google.code.gson", module = "gson")
    }
    include("io.jsonwebtoken:jjwt-gson:0.13.0") {
        exclude(group = "com.google.code.gson", module = "gson")
    }
    implementation("net.lenni0451:Reflect:1.6.3")
    include("net.lenni0451:Reflect:1.6.3")
    implementation("net.lenni0451.commons:unchecked:1.9.2")
    include("net.lenni0451.commons:unchecked:1.9.2")
    implementation("de.florianreuth:classic4j:2.3.0")
    include("de.florianreuth:classic4j:2.3.0")
    implementation("net.raphimc:MinecraftAuth:5.0.1") {
        exclude(group = "com.google.code.gson", module = "gson")
    }
    include("net.raphimc:MinecraftAuth:5.0.1") {
        exclude(group = "com.google.code.gson", module = "gson")
    }
    implementation("dev.kastle.netty:netty-transport-raknet:1.7.0") { exclude(group = "io.netty") }
    include("dev.kastle.netty:netty-transport-raknet:1.7.0") { exclude(group = "io.netty") }
    implementation("dev.kastle.netty:netty-transport-nethernet:1.7.0") { exclude(group = "io.netty") }
    include("dev.kastle.netty:netty-transport-nethernet:1.7.0") { exclude(group = "io.netty") }
    implementation("dev.kastle.webrtc:webrtc-java:1.0.3:$webRtcPlatform")
    include("dev.kastle.webrtc:webrtc-java:1.0.3:$webRtcPlatform")

    val skijaVersion = "0.143.17"
    implementation("io.github.humbleui:skija-windows-x64:$skijaVersion")
    include("io.github.humbleui:skija-windows-x64:$skijaVersion")
    implementation("io.github.humbleui:skija-linux-x64:$skijaVersion")
    include("io.github.humbleui:skija-linux-x64:$skijaVersion")

    nestedLibraryModules.forEach { include("dioxidelite.nested:$it:1.0.0") }
}

tasks.withType<Test>().configureEach { useJUnitPlatform() }

loom {
    val aw = file("src/main/resources/${modId}.accesswidener")
    if (aw.exists()) accessWidenerPath.set(aw)
    runs {
        named("client") {
            client()
            configName = "Fabric Client"
            ideConfigGenerated(true)
            runDir("runs/client")
        }
    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(javaVersion))
    withSourcesJar()
    sourceCompatibility = JavaVersion.toVersion(javaVersion)
    targetCompatibility = JavaVersion.toVersion(javaVersion)
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(javaVersion.toInt())
}

tasks.named<ProcessResources>("processResources") {
    val props = mapOf(
            "version" to version,
            "mod_id" to modId,
            "mod_name" to modName,
            "mod_author" to modAuthor,
            "license" to project.property("license").toString(),
            "description" to (project.findProperty("description")?.toString() ?: ""),
            "minecraft_version" to minecraftVersion,
            "fabric_loader_version" to project.property("fabric_loader_version").toString(),
            "java_version" to javaVersion
    )
    inputs.properties(props)
    filesMatching(listOf("fabric.mod.json")) { expand(props) }
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(
                "Specification-Title" to modName,
                "Specification-Vendor" to modAuthor,
                "Implementation-Title" to project.name,
                "Implementation-Version" to archiveVersion,
                "Implementation-Vendor" to modAuthor,
                "Built-On-Minecraft" to minecraftVersion
        )
    }
}
