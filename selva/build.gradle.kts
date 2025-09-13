plugins {
    alias(libs.plugins.micronaut.minimal.application)
    alias(libs.plugins.micronaut.docker)
    alias(libs.plugins.micronaut.testresources)
    alias(libs.plugins.shadow)
    alias(libs.plugins.flyway)
    alias(libs.plugins.jooq)
    idea
}

buildscript {
    dependencies {
        classpath(libs.flyway.database.postgresql)
    }
}

version = "1.0-SNAPSHOT"
group = "org.ivanvyazmitinov"

repositories {
    mavenCentral()
}

dependencies {
    annotationProcessor(mn.validation)
    annotationProcessor(mn.micronaut.openapi)
    annotationProcessor(mn.micronaut.security.annotations)
    annotationProcessor(mn.micronaut.serde.processor)
    annotationProcessor(mn.micronaut.validation.processor)
    annotationProcessor(mn.micronaut.data.processor)
    implementation(mn.micronaut.data.jdbc)

    implementation(mn.micrometer.context.propagation)
    implementation(mn.micronaut.http.client.jdk)
    implementation(mn.micronaut.jackson.databind)
    implementation(mn.micronaut.retry)
    implementation(mn.micronaut.websocket)
    implementation(mn.micronaut.cache.caffeine)
    implementation(mn.micronaut.flyway)
    implementation(mn.micronaut.reactor)
    implementation(mn.micronaut.security)
    implementation(mn.micronaut.security.session)
    implementation(mn.micronaut.serde.jackson)
    implementation(mn.micronaut.jdbc.hikari)
    implementation(mn.micronaut.jooq)
    implementation(mn.micronaut.toml)
    implementation(mn.micronaut.validation)
    implementation(mn.micronaut.views.fieldset)
    implementation(mn.micronaut.views.thymeleaf)
    implementation(mn.jakarta.annotation.api)
    implementation(mn.validation)

    implementation(libs.jooq)
    implementation(libs.spring.security.crypto)
    implementation(libs.jcl)
    implementation(libs.tika)
    implementation(libs.guava)
    compileOnly(mn.micronaut.openapi.annotations)

    jooqCodegen(mn.postgresql)

    runtimeOnly(mn.logback.classic)
    runtimeOnly(mn.flyway.postgresql)
    runtimeOnly(mn.postgresql)
    runtimeOnly(mn.snakeyaml)

    testImplementation(mn.testcontainers.postgres)
    testImplementation(mn.micronaut.test.resources.extensions.junit.platform)
    testImplementation(mn.micronaut.test.junit5)
    testImplementation(libs.playwrite)
    testImplementation(libs.testcontainers.junit)
}


application {
    mainClass = "org.ivanvyazmitinov.selva.Application"
}

java {
    sourceCompatibility = JavaVersion.toVersion("21")
    targetCompatibility = JavaVersion.toVersion("21")
}

micronaut {
    runtime("netty")
    testRuntime("junit5")
    processing {
        incremental(false)
    }
}

idea.module {
    isDownloadJavadoc = true
    isDownloadSources = true
}

flyway {
    url = "jdbc:postgresql://localhost:5432/postgres"
    user = "selva_service"
    password = "selva"
    schemas = arrayOf("selva_migrations", "selva")
    password = ""
}

jooq {
    configuration {
        jdbc {
            driver = "org.postgresql.Driver"
            url = "jdbc:postgresql://localhost:5432/postgres"
            user = "postgres"
        }
        generator {
            generate {
                withPojos(true)
                withPojosAsJavaRecordClasses(true)
                withSequences(true)
            }
            database {
                name = "org.jooq.meta.postgres.PostgresDatabase"
                includes = """selva.*"""
                excludes = """selva_migrations.*"""
                withIncludeSystemSequences(true)
            }
            target {
                directory = "src/main/java"
                packageName = "org.ivanvyazmitinov.selva.repository.jooq.generated"
            }
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

afterEvaluate {
    val generateJooqTask = tasks.findByPath("jooqCodegen")
    val flywayMigrateTask = tasks.findByPath("flywayMigrate")
    if (file("src/main/resources/db/migration").exists()) {
        if (generateJooqTask != null && flywayMigrateTask != null) {
            generateJooqTask.shouldRunAfter(flywayMigrateTask)
            tasks.register("generateJooqWithFlywayMigrate") {
                group = "jooq"
                dependsOn(generateJooqTask, flywayMigrateTask)
            }
        }
    } else {
        generateJooqTask?.enabled = false
        flywayMigrateTask?.enabled = false
    }
}

tasks.register<JavaExec>("playwrightCodegen"){
    group = "verification"
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass = "com.microsoft.playwright.CLI"
    args = listOf("codegen", "http://127.0.0.1:8080", "--viewport-size", "1920,1080")
}

val playwrightInstallDeps = tasks.register<JavaExec>("playwrightShowDeps") {
    group = "build"
    classpath = sourceSets.test.get().compileClasspath
    mainClass = "com.microsoft.playwright.CLI"
    args = listOf("install-deps", "--dry-run")
}