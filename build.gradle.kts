import org.gradle.api.plugins.JavaPluginExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    kotlin("jvm") version "2.3.0"
    kotlin("plugin.spring") version "2.3.0"
    id("org.springframework.boot") version "3.5.9"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.mcp"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
    maven { url = uri("https://repo.spring.io/milestone") }
}

extensions.configure<JavaPluginExtension> {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.ai:spring-ai-starter-mcp-server-webmvc:1.1.0-M3")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.named<BootJar>("bootJar") {
    archiveBaseName.set("mcp-client")
}
