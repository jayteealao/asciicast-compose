plugins {
    kotlin("jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    api(project(":vt-api"))

    // TODO: Add JediTerm dependency when implementing
    // implementation("org.jetbrains.jediterm:jediterm-pty:...")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
