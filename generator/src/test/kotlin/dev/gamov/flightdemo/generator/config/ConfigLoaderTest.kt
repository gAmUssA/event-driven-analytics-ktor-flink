package dev.gamov.flightdemo.generator.config

import com.sksamuel.hoplite.ConfigLoader
import com.sksamuel.hoplite.PropertySource
import com.sksamuel.hoplite.yaml.YamlPropertySource
import org.junit.jupiter.api.Test
import java.io.File

class ConfigLoaderTest {

    @Test
    fun testLoadConfig() {
        val configFile = "generator/src/main/resources/generator-config.yaml"
        val file = File(configFile)
        
        println("File exists: ${file.exists()}")
        println("File absolute path: ${file.absolutePath}")
        
        try {
            val config = ConfigLoader.builder()
                .addSource(YamlPropertySource(file.absolutePath))
                .addSource(PropertySource.environment())
                .build()
                .loadConfigOrThrow<GeneratorConfig>()
            
            println("Config loaded successfully:")
            println("Kafka config: ${config.kafka}")
            println("Simulation config: ${config.simulation}")
        } catch (e: Exception) {
            println("Error loading config: ${e.message}")
            e.printStackTrace()
        }
    }
}