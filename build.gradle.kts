// Compiles the Kotlin fences of the deck. `npm run snippets` (slidev-kotlin-snippets,
// shipped by slidev-theme-kotlin) writes one file per fence under
// src/main/kotlin/presentation/snippets/; src/main/kotlin/presentation/support/ is the
// hand-written context those fences use without defining.
plugins {
  id("org.jetbrains.kotlin.jvm") version "2.4.20"
  id("org.jetbrains.kotlin.plugin.serialization") version "2.4.20"
}

val ktorVersion = "3.5.2"

repositories {
  mavenCentral()
}

dependencies {
  // Server
  implementation("io.ktor:ktor-server-core:$ktorVersion")
  implementation("io.ktor:ktor-server-netty:$ktorVersion")
  implementation("io.ktor:ktor-server-cio:$ktorVersion")
  implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
  implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
  implementation("io.ktor:ktor-serialization-kotlinx-xml:$ktorVersion")
  implementation("io.ktor:ktor-server-html-builder:$ktorVersion")
  implementation("io.ktor:ktor-server-resources:$ktorVersion")
  implementation("io.ktor:ktor-server-compression:$ktorVersion")
  implementation("io.ktor:ktor-server-cors:$ktorVersion")
  implementation("io.ktor:ktor-server-default-headers:$ktorVersion")
  implementation("io.ktor:ktor-server-sessions:$ktorVersion")
  implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
  implementation("io.ktor:ktor-server-auth:$ktorVersion")
  implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
  implementation("io.ktor:ktor-server-auth-ldap:$ktorVersion")
  implementation("io.ktor:ktor-server-websockets:$ktorVersion")
  implementation("io.ktor:ktor-server-metrics-micrometer:$ktorVersion")
  implementation("io.ktor:ktor-server-config-yaml:$ktorVersion")
  implementation("io.ktor:ktor-server-di:$ktorVersion")
  implementation("io.ktor:ktor-server-openapi:$ktorVersion")
  implementation("io.ktor:ktor-server-swagger:$ktorVersion")
  implementation("io.ktor:ktor-server-routing-openapi:$ktorVersion")
  implementation("io.ktor:ktor-server-test-host:$ktorVersion")

  // Client
  implementation("io.ktor:ktor-client-core:$ktorVersion")
  implementation("io.ktor:ktor-client-cio:$ktorVersion")
  implementation("io.ktor:ktor-client-resources:$ktorVersion")
  implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
  implementation("io.ktor:ktor-client-websockets:$ktorVersion")

  // Libraries the slides lean on
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
  implementation("org.jetbrains.kotlinx:kotlinx-html:0.12.0")
  implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.8.0")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
  implementation("io.micrometer:micrometer-registry-prometheus:1.17.1")
  implementation("com.auth0:java-jwt:4.6.0")
  implementation("com.sksamuel.hoplite:hoplite-core:2.9.0")
  implementation("com.sksamuel.hoplite:hoplite-hocon:2.9.0")
  implementation("ch.qos.logback:logback-classic:1.6.3")
  implementation("io.kotest:kotest-runner-junit5:6.2.4")
  implementation("io.kotest:kotest-assertions-core:6.2.4")
  implementation("io.kotest:kotest-property:6.2.4")
}

kotlin {
  jvmToolchain(21)
  compilerOptions.freeCompilerArgs.addAll("-Xcontext-parameters", "-Xcollection-literals")
}

val snippetsCheck by tasks.register<Exec>("snippetsCheck") {
  commandLine("npx", "slidev-kotlin-snippets", "--check")
}

tasks.check { dependsOn(snippetsCheck) }
