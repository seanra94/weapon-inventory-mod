import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File

plugins {
    kotlin("jvm") version "2.1.20"
    java
}

group = "weaponsprocurement"
version = "0.2.0"

repositories {
    mavenCentral()
}

fun configuredPath(name: String): String? =
    (findProperty(name) as String?)?.takeIf { it.isNotBlank() }

fun environmentPath(name: String): String? =
    System.getenv(name)?.takeIf { it.isNotBlank() }

fun hasStarsectorCoreJars(dir: File): Boolean =
    File(dir, "starfarer.api.jar").isFile &&
        File(dir, "log4j-1.2.9.jar").isFile &&
        File(dir, "lwjgl.jar").isFile &&
        File(dir, "json.jar").isFile

val starsectorDirectoryPath: String =
    configuredPath("starsectorDir")
        ?: environmentPath("STARSECTOR_DIRECTORY")
        ?: error(
            """
            Starsector directory not configured.

            Set one of:
              - Gradle property: -PstarsectorDir=C:\Path\To\Starsector
              - Environment variable: STARSECTOR_DIRECTORY=C:\Path\To\Starsector
              - build.ps1 parameter: -StarsectorDir C:\Path\To\Starsector
            """.trimIndent()
        )

val starsectorDirectory = file(starsectorDirectoryPath)
val starsectorCoreDirectory = File(starsectorDirectory, "starsector-core")

data class RequiredMod(
    val displayName: String,
    val folderPrefixes: List<String>,
    val modIds: List<String>,
)

fun modInfoContainsId(candidate: File, modId: String): Boolean {
    val modInfo = File(candidate, "mod_info.json")
    if (!modInfo.isFile) return false
    return Regex(""""id"\s*:\s*"${Regex.escape(modId)}"""", RegexOption.IGNORE_CASE)
        .containsMatchIn(modInfo.readText())
}

fun resolveDependencyModDirectory(mod: RequiredMod): File {
    val modsDirectory = File(starsectorDirectory, "mods")
    if (!modsDirectory.isDirectory) {
        error("Could not find Starsector mods directory: ${modsDirectory.absolutePath}")
    }

    val candidates = modsDirectory.listFiles()
        ?.filter { it.isDirectory }
        .orEmpty()
        .sortedByDescending { it.name }

    candidates.firstOrNull { candidate ->
        mod.folderPrefixes.any { prefix -> candidate.name.equals(prefix, ignoreCase = true) }
    }?.let { return it }

    candidates.firstOrNull { candidate ->
        mod.folderPrefixes.any { prefix -> candidate.name.startsWith("$prefix-", ignoreCase = true) }
    }?.let { return it }

    candidates.firstOrNull { candidate ->
        mod.modIds.any { id -> modInfoContainsId(candidate, id) }
    }?.let { return it }

    error(
        """
        Missing required dependency mod: ${mod.displayName}

        Install it into:
          ${modsDirectory.absolutePath}

        Accepted folder prefixes:
          ${mod.folderPrefixes.joinToString(", ")}
        Accepted mod ids:
          ${mod.modIds.joinToString(", ")}
        """.trimIndent()
    )
}

val lunaLibDirectory = resolveDependencyModDirectory(
    RequiredMod(
        displayName = "LunaLib",
        folderPrefixes = listOf("LunaLib"),
        modIds = listOf("lunalib"),
    )
)
val lazyLibDirectory = resolveDependencyModDirectory(
    RequiredMod(
        displayName = "LazyLib",
        folderPrefixes = listOf("LazyLib"),
        modIds = listOf("lw_lazylib"),
    )
)

fun dependencyJarTree(directory: File) =
    fileTree(File(directory, "jars")) {
        include("*.jar")
        include("internal/*.jar")
    }

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib:2.1.20")
    compileOnly(
        fileTree(starsectorCoreDirectory) {
            include("starfarer.api.jar")
            include("log4j-1.2.9.jar")
            include("lwjgl.jar")
            include("lwjgl_util.jar")
            include("json.jar")
        }
    )
    compileOnly(dependencyJarTree(lunaLibDirectory))
    compileOnly(dependencyJarTree(lazyLibDirectory))
}

sourceSets {
    named("main") {
        java.setSrcDirs(listOf("src"))
        java.exclude("main/**")
        java.exclude("privateBadge/**")
        kotlin.srcDirs("src/main/kotlin")
        resources.setSrcDirs(emptyList<String>())
    }

    // PRIVATE_BADGE_GRADLE_START
    create("privateBadge") {
        java.srcDirs("src/privateBadge/java")
        kotlin.srcDirs("src/privateBadge/kotlin")
        compileClasspath += sourceSets["main"].output + sourceSets["main"].compileClasspath
        runtimeClasspath += output + compileClasspath
    }
    // PRIVATE_BADGE_GRADLE_END
}

tasks.register("validateLocalBuildEnvironment") {
    group = "verification"
    description = "Checks Starsector and dependency mod paths used for local Weapons Procurement builds."

    doLast {
        println("Starsector directory: ${starsectorDirectory.absolutePath}")
        println("Starsector core: ${starsectorCoreDirectory.absolutePath}")
        println("LunaLib: ${lunaLibDirectory.absolutePath}")
        println("LazyLib: ${lazyLibDirectory.absolutePath}")

        require(starsectorDirectory.isDirectory) {
            "Missing Starsector directory: ${starsectorDirectory.absolutePath}"
        }
        require(starsectorCoreDirectory.isDirectory) {
            "Missing Starsector core directory: ${starsectorCoreDirectory.absolutePath}"
        }
        require(hasStarsectorCoreJars(starsectorCoreDirectory)) {
            "Missing required Starsector core jars in: ${starsectorCoreDirectory.absolutePath}"
        }
        listOf("LunaLib" to lunaLibDirectory, "LazyLib" to lazyLibDirectory).forEach { (name, directory) ->
            val jarsDirectory = File(directory, "jars")
            require(jarsDirectory.isDirectory) {
                "$name has no jars directory: ${jarsDirectory.absolutePath}"
            }
            require(dependencyJarTree(directory).files.isNotEmpty()) {
                "$name has no compile jars under: ${jarsDirectory.absolutePath}"
            }
        }
    }
}

tasks.withType<JavaCompile>().configureEach {
    dependsOn("validateLocalBuildEnvironment")
    options.encoding = "UTF-8"
}

tasks.withType<KotlinCompile>().configureEach {
    dependsOn("validateLocalBuildEnvironment")
    compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
}

java.sourceCompatibility = JavaVersion.VERSION_17
java.targetCompatibility = JavaVersion.VERSION_17

tasks.named<Jar>("jar") {
    dependsOn("validateLocalBuildEnvironment")
    archiveFileName.set("weapons-procurement.jar")
    destinationDirectory.set(layout.projectDirectory.dir("jars"))
    from(sourceSets["main"].output)
}

// PRIVATE_BADGE_GRADLE_START
tasks.register<Jar>("privateBadgeJar") {
    dependsOn("validateLocalBuildEnvironment", "classes", "privateBadgeClasses")
    archiveFileName.set("weapons-procurement.jar")
    destinationDirectory.set(layout.projectDirectory.dir("jars"))
    from(sourceSets["main"].output)
    from(sourceSets["privateBadge"].output)
}

tasks.register("buildPrivateMod") {
    group = "build"
    description = "Builds the private jar including the optional patched-badge source set."
    dependsOn("privateBadgeJar")
}
// PRIVATE_BADGE_GRADLE_END

tasks.register("buildMod") {
    group = "build"
    description = "Builds the public-safe clean Weapons Procurement jar."
    dependsOn("jar")
}
