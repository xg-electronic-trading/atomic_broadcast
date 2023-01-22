plugins {
    java
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.bundles.real.logic)
    implementation(libs.gflog.api)
    runtimeOnly(libs.gflog.core)

    implementation(project(":message-encoders"))
    implementation(project(":zgc-utilities"))
    implementation(project(":transport"))
    implementation(project(":algo-container"))

    testImplementation(project(":message-encoders"))
    testImplementation(project(":zgc-utilities"))
    testImplementation(project(":transport"))
    testImplementation(project(":algo-container"))

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
    testLogging {
        events("passed")
    }
}