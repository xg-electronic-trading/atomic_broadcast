plugins {
    java
    `java-library`
}

version = "unspecified"

repositories {
    mavenCentral()
}

val codecGeneration: Configuration by configurations.creating

dependencies {
    val sbeVersion = libs.sbe.all.get().versionConstraint.requiredVersion
    codecGeneration("uk.co.real-logic:sbe-tool:${sbeVersion}")
    implementation(libs.bundles.real.logic)
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.register<JavaExec>("generateEncoders") {
    val codecsFile = "src/main/resources/fix/messages.xml"
    val sbeFile = "src/main/resources/sbe/sbe.xsd"
    val outputDir = "${buildDir}/generated-src"

    inputs.files(codecsFile, sbeFile)
    outputs.dir(outputDir)

    mainClass.set("uk.co.real_logic.sbe.SbeTool")
    classpath = configurations.getByName("codecGeneration")
    systemProperties(
                    "sbe.output.dir" to outputDir.toString(),
                    "sbe.target.language" to "Java",
                    "sbe.validation.stop.on.error" to "true",
                    "sbe.validation.xsd" to "src/main/resources/sbe/sbe.xsd",
                    "sbe.java.generate.interfaces" to "true")

    args = listOf(codecsFile)
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
    testLogging {
        events("passed")
    }
}

tasks.named<JavaCompile>("compileJava") {
    dependsOn("generateEncoders")
}

java.sourceSets["main"].java {
    srcDirs("src/main/java", "build/generated-src")
}
java.sourceSets["test"].java {
    srcDirs("src/test/java", "build/generated-src")
}
