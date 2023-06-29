plugins {
    java
    `maven-publish`
    signing
}

group = "io.github.xg-electronic-trading"
version = "UNVERSIONED-SNAPSHOT"

java {
    withJavadocJar()
    withSourcesJar()
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":message-encoders"))
    implementation(project(":transport"))
    implementation(project(":algo-container"))
    implementation(project(":zgc-utilities"))
}

publishing {
    publications {
        create<MavenPublication>("atomicBroadcastAll") {
            artifactId = "atomic-broadcast-all"
            from(components["java"])
            versionMapping {
                usage("java-api") {
                    fromResolutionOf("runtimeClasspath")
                }
                usage("java-runtime") {
                    fromResolutionResult()
                }
            }
            pom {
                name.set("Atomic Broadcast All")
                description.set("Zero GC Low Latency Sequencer and Algo Container")
                url.set("https://github.com/xg-electronic-trading/atomic_broadcast")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("feyyaz-91")
                        name.set("Feyyaz Alam")
                        email.set("feyyaz_alam@hotmail.co.uk")
                    }
                }
                scm {
                    connection.set("scm:git:https://github.com/xg-electronic-trading/atomic_broadcast.git")
                    developerConnection.set("scm:git:https://github.com/xg-electronic-trading/atomic_broadcast.git")
                    url.set("https://github.com/xg-electronic-trading/atomic_broadcast.git")
                }
            }
        }
    }
    repositories {
        maven {
            // change URLs to point to your repos, e.g. http://my.org/repo
            val releasesRepoUrl = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
            val snapshotsRepoUrl = uri("https://oss.sonatype.org/content/repositories/snapshots/")
            url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
            credentials {
                username = System.getenv("MAVEN_USERNAME")
                password = System.getenv("MAVEN_PASSWORD")
            }
        }
    }
}