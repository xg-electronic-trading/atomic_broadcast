plugins {
    java
}

version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":transport"))
    implementation(project(":zgc-utilities"))
    testImplementation(project(":transport"))
    testImplementation(project(":zgc-utilities"))

    // Use JUnit Jupiter for testing.
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.1")

    // This dependency is used by the application.
    implementation("com.google.guava:guava:30.1.1-jre")
    implementation(libs.bundles.real.logic)

    implementation(libs.gflog.api)
    runtimeOnly(libs.gflog.core)

}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}