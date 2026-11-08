plugins {
    id("everybody.codes") apply false
}

subprojects {
    if (parent?.parent?.name == "events") {
        apply(plugin = "everybody.codes")
        repositories {
            mavenCentral()
        }
        tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile>().configureEach {
            jvmTargetValidationMode.set(org.jetbrains.kotlin.gradle.dsl.jvm.JvmTargetValidationMode.IGNORE)
        }
    }
}