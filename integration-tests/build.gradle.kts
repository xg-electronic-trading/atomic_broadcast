plugins {
    java
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":zgc-utilities"))
    implementation(project(":transport"))
    implementation(project(":algo-container"))

    testImplementation(project(":zgc-utilities"))
    implementation(project(":transport"))
    testImplementation(project(":algo-container"))

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
    testLogging {
        events("passed")
    }
}