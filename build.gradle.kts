plugins {
    kotlin("jvm") version "2.4.21"
    kotlin("plugin.serialization") version "2.4.21"
}

val generator by configurations.creating
val generated = layout.buildDirectory.dir("generated")

val openApiGenerate by tasks.registering(JavaExec::class) {
    val spec = layout.projectDirectory.file("openapi.yaml")
    inputs.file(spec)
    outputs.dir(generated)
    doFirst { delete(generated) }
    classpath = generator
    mainClass = "org.openapitools.codegen.OpenAPIGenerator"
    args(
        "generate",
        "-g", "kotlin",
        "--library", "jvm-retrofit2",
        "-i", spec.asFile.path,
        "-o", generated.get().asFile.path,
        "--package-name", "repro",
        "--additional-properties=serializationLibrary=kotlinx_serialization,useCoroutines=true,generateOneOfAnyOfWrappers=true",
        "--type-mappings=integer=kotlin.Long,number=kotlin.Double",
    )
}

sourceSets.main {
    kotlin.srcDir(openApiGenerate.map { generated.get().dir("src/main/kotlin") })
}

dependencies {
    generator("org.openapitools:openapi-generator-cli:7.27.0-20261030.091743-151")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.12.0")
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-kotlinx-serialization:3.0.0")
    implementation("com.squareup.retrofit2:converter-scalars:3.0.0")
    implementation("com.squareup.okhttp3:logging-interceptor:5.5.1")
}
