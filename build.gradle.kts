plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
    id("org.jetbrains.intellij.platform") version "2.5.0"
    id("org.jetbrains.grammarkit") version "2022.3.2.2"
}

group = "io.github.braams"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

sourceSets["main"].java.srcDirs("src/main/gen")

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
dependencies {
    intellijPlatform {
        create("IC", "2025.1")
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)

        // Add necessary plugin dependencies for compilation here, example:
        // bundledPlugin("com.intellij.java")
    }
    testImplementation("junit:junit:4.13.2")
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "251"
        }

        changeNotes = """
      Initial version
    """.trimIndent()
    }
}

tasks {
    generateLexer {
        sourceFile.set(file("src/main/grammar/Kamailio.flex"))
        targetOutputDir.set(file("src/main/gen/io/github/braams/kamailio/lexer"))
        purgeOldFiles.set(true)
    }
    generateParser {
        sourceFile.set(file("src/main/grammar/Kamailio.bnf"))
        targetRootOutputDir.set(file("src/main/gen"))
        pathToParser.set("io/github/braams/kamailio/parser/KamailioParser.java")
        pathToPsiRoot.set("io/github/braams/kamailio/psi")
        purgeOldFiles.set(true)
    }

    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
        dependsOn(generateLexer, generateParser)
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "21"
        dependsOn(generateLexer, generateParser)
    }
}
