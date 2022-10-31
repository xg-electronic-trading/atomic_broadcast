plugins {
    java
}

version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":zgc-utilities"))

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")

    implementation(libs.bundles.jmh)
    annotationProcessor("org.openjdk.jmh:jmh-generator-annprocess:1.35")
    implementation(libs.gflog.api)
    runtimeOnly(libs.gflog.core)
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}