// ---------------------------------------------------------------------------
// DioxideLite —— 由 SetsunaClient 的构建脚本改写而来。
//
// 与上游的差异（DioxideLite 是单 mod 结构）：
//   1. 上游把 SetsunaVia 作为 src/vendored/setsunavia 的独立源码集并产出多个
//      mod（setsunavia / setsunavia-api / setsunavia-visuals）。DioxideLite 把
//      协议翻译层直接内联在 com/viaversion/setsunavia/** 下，fabric.mod.json
//      也只有一个 mod，因此这里不需要额外的 srcDirs 与 processResources 合并。
//   2. 上游的 nested 库（67 个）通过 flatDir libs/nested 引入。这一段默认关闭，因为它和
//      下面的 Maven include 完全重复（skija / webrtc / viaversion / netty 各会打进两份），
//      需要复现上游 dev 打包方式时加 -Puse_nested_libs=true。
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
val useNestedLibs = providers.gradleProperty("use_nested_libs").orElse("false").get().toBoolean()
val hasNestedLibs = nestedLibDir.isDirectory && useNestedLibs
val nestedLibraryModules = if (hasNestedLibs) (1..67).map { "nested-%03d".format(it) } else emptyList()

// ---------------------------------------------------------------------------
// 原生库平台探测
//
// DioxideLite 有两个带原生代码的依赖：
//   * Skija（GPU 自绘 UI 的 Skia 绑定）
//   * webrtc-java（基岩版 NetherNet / 语音通道）
// 两者的产物都按 操作系统 + CPU 架构 分开发布，且都只提供“原生那部分”，Java 类在另一个
// artifact 里。发布包必须同时打进 [Java 类 + 对应平台原生库]，否则运行期会
// NoClassDefFoundError / UnsatisfiedLinkError。默认值按构建机自动探测，交叉打包时用
// -Pskija_platforms=... / -Pwebrtc_platform=... 覆盖。
// ---------------------------------------------------------------------------
val supportedSkijaPlatforms = setOf(
        "skija-windows-x64", "skija-windows-arm64",
        "skija-linux-x64", "skija-linux-arm64",
        "skija-macos-arm64", "skija-macos-x64"
)
val supportedWebRtcPlatforms = setOf(
        "windows-x86_64", "windows-aarch64",
        "linux-x86_64", "linux-aarch64",
        "macos-aarch64", "macos-x86_64"
)

fun hostOsName(): String = System.getProperty("os.name", "").lowercase()
fun hostArchName(): String = System.getProperty("os.arch", "").lowercase()
fun hostIsArm(): Boolean = hostArchName().contains("aarch64") || hostArchName().contains("arm64")

fun defaultSkijaPlatforms(): String = when {
    hostOsName().contains("win") -> if (hostIsArm()) "skija-windows-arm64" else "skija-windows-x64"
    hostOsName().contains("mac") -> if (hostIsArm()) "skija-macos-arm64" else "skija-macos-x64"
    else -> if (hostIsArm()) "skija-linux-arm64" else "skija-linux-x64"
}

fun defaultWebRtcPlatform(): String = when {
    hostOsName().contains("win") -> if (hostIsArm()) "windows-aarch64" else "windows-x86_64"
    hostOsName().contains("mac") -> if (hostIsArm()) "macos-aarch64" else "macos-x86_64"
    else -> if (hostIsArm()) "linux-aarch64" else "linux-x86_64"
}

val modMenuLocalJar = file(providers.gradleProperty("modmenu_jar")
        .orElse("libs/modmenu-18.0.0-alpha.8.jar")
        .get())

// webrtc_platforms（逗号分隔）可以一次打进多个平台的原生库，做三平台通用包：
//   -Pwebrtc_platforms=windows-x86_64,linux-x86_64,macos-aarch64,macos-x86_64
// 兼容旧的单值写法 webrtc_platform=...；都没写时按构建机探测。
val webRtcPlatforms = providers.gradleProperty("webrtc_platforms")
        .orElse(providers.gradleProperty("webrtc_platform").orElse(defaultWebRtcPlatform()))
        .get()
        .split(',')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .also { platforms ->
            require(platforms.isNotEmpty()) { "webrtc_platforms must list at least one platform" }
            platforms.forEach { require(it in supportedWebRtcPlatforms) { "Unsupported webrtc platform: $it" } }
        }

val skijaPlatforms = providers.gradleProperty("skija_platforms")
        .orElse(defaultSkijaPlatforms())
        .get()
        .split(',')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .also { platforms ->
            require(platforms.isNotEmpty()) { "skija_platforms must list at least one platform" }
            platforms.forEach { require(it in supportedSkijaPlatforms) { "Unsupported skija platform: $it" } }
        }

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

    // 2.2.6 视频背景烘焙（VideoBackgroundBaker）依赖
    implementation("org.jcodec:jcodec:0.2.5")
    include("org.jcodec:jcodec:0.2.5")
    implementation("org.jcodec:jcodec-javase:0.2.5")
    include("org.jcodec:jcodec-javase:0.2.5")

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
    // webrtc-java: 同样是「Java 类 + 平台原生库」两个 artifact，两个都要 include。
    implementation("dev.kastle.webrtc:webrtc-java:1.0.3")
    include("dev.kastle.webrtc:webrtc-java:1.0.3")
    webRtcPlatforms.forEach { platform ->
        implementation("dev.kastle.webrtc:webrtc-java:1.0.3:$platform")
        include("dev.kastle.webrtc:webrtc-java:1.0.3:$platform")
    }

    // Skija: Java API 与平台原生库是两个 artifact，发布包两个都要 include。
    // 0.2.1 的发布包漏了 skija-shared，运行期一进 UI 就 NoClassDefFoundError。
    val skijaVersion = "0.143.17"
    implementation("io.github.humbleui:skija-shared:$skijaVersion")
    include("io.github.humbleui:skija-shared:$skijaVersion")
    implementation("io.github.humbleui:types:0.2.0")
    include("io.github.humbleui:types:0.2.0")
    skijaPlatforms.forEach { platform ->
        implementation("io.github.humbleui:$platform:$skijaVersion")
        include("io.github.humbleui:$platform:$skijaVersion")
    }

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
