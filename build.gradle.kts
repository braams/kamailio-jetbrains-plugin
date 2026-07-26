plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
    id("org.jetbrains.intellij.platform") version "2.5.0"
    id("org.jetbrains.grammarkit") version "2022.3.2.2"
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

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
            // no upper bound: the plugin only uses stable platform APIs
            untilBuild = provider { null }
        }

        changeNotes = """
      <h3>1.1.0</h3>
      <ul>
        <li>Keywords used as names inside pseudo-variable keys and transformation arguments
            (e.g. <code>${'$'}var(route)</code>, <code>${'$'}sht(h=&gt;status)</code>) are now parsed,
            highlighted and hovered as plain identifiers instead of keywords</li>
        <li>Pseudo-variables and transformations are attributed to their exporting module in
            hover and completion</li>
        <li>Greatly expanded bundled documentation for core and modules</li>
      </ul>
      <h3>1.0.0</h3>
      <ul>
        <li>Syntax highlighting, structure view and code folding for Kamailio configuration files</li>
        <li>Hover documentation for core parameters, functions, keywords, module functions and parameters,
            pseudo-variables and transformations</li>
        <li>Go to definition for routes, #!define constants and cross-file targets via include_file</li>
        <li>Formatter mirroring the stock config style</li>
        <li>Semantic inspections (unresolved/duplicate routes, modparam without loadmodule, unbalanced ifdef)</li>
      </ul>
    """.trimIndent()
    }

    // Optional plugin signing: only wired when the certificate secrets are present
    // (so local builds and first-time publishes work without a certificate).
    if (System.getenv("CERTIFICATE_CHAIN") != null) {
        signing {
            certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
            privateKey = providers.environmentVariable("PRIVATE_KEY")
            password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
        }
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
    }

    pluginVerification {
        ides {
            ide(org.jetbrains.intellij.platform.gradle.IntelliJPlatformType.IntellijIdeaCommunity, "2025.1")
            ide(org.jetbrains.intellij.platform.gradle.IntelliJPlatformType.IntellijIdeaCommunity, "2025.2")
        }
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
