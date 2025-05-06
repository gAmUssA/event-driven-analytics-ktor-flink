package dev.gamov.flightdemo.generator.config

import org.junit.jupiter.api.Test
import java.io.File

class YamlFileTest {

    @Test
    fun testReadYamlFile() {
        val configFile = "generator/src/main/resources/generator-config.yaml"
        val file = File(configFile)
        
        println("File exists: ${file.exists()}")
        println("File absolute path: ${file.absolutePath}")
        println("File can read: ${file.canRead()}")
        println("File size: ${file.length()} bytes")
        
        if (file.exists() && file.canRead()) {
            val content = file.readText()
            println("File content:")
            println(content)
        } else {
            println("Cannot read file")
        }
    }
}