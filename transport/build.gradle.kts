plugins {
    java
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

dependencies {
    // Use JUnit Jupiter for testing.
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.1")
    testImplementation(project(":transport"))

    // This dependency is used by the application.
    implementation("com.google.guava:guava:30.1.1-jre")
    implementation(libs.bundles.real.logic)

    implementation(libs.gflog.api)
    runtimeOnly(libs.gflog.core)
}

tasks.named<Test>("test") {
    // Use JUnit Platform for unit tests.
    useJUnitPlatform()
}
