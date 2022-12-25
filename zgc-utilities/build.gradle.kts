plugins {
    java
}

version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    implementation(libs.bundles.real.logic)
    implementation(libs.gflog.api)
    runtimeOnly(libs.gflog.core)
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
    testLogging {
        events("passed")
    }
}