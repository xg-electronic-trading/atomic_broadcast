/*
 * This file was generated by the Gradle 'init' task.
 *
 * The settings file is used to specify which projects to include in your build.
 *
 * Detailed information about configuring a multi-project build in Gradle can be found
 * in the user manual at https://docs.gradle.org/7.4.2/userguide/multi_project_builds.html
 */

rootProject.name = "atomic-broadcast"
include("message-encoders")
include("transport")
include("algo-container")
include("perf-benchmarks")
include("zgc-utilities")
include("integration-tests")
include("atomic-broadcast-all")


dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            library("aeron-all", "io.aeron", "aeron-all").version("1.42.1")
            library("sbe-all", "uk.co.real-logic", "sbe-all").version("1.29.0")
            library("agrona", "org.agrona", "agrona").version("1.19.2")
            library("gflog-api", "com.epam.deltix", "gflog-api").version("3.0.2")
            library("gflog-core", "com.epam.deltix", "gflog-core").version("3.0.2")
            library("jmh-core", "org.openjdk.jmh", "jmh-core").version("1.35")
            library("jmh-generator-annprocess", "org.openjdk.jmh", "jmh-generator-annprocess").version("1.35")
            library("config", "com.typesafe", "config").version("1.4.2")
            bundle("real-logic", listOf("aeron-all", "sbe-all", "agrona"))
            bundle("jmh", listOf("jmh-core", "jmh-generator-annprocess"))
        }
    }
}
