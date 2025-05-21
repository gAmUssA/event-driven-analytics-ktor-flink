package dev.gamov.flightdemo.ktor

import dev.gamov.flightdemo.ktor.config.DatabaseConfig
import dev.gamov.flightdemo.ktor.config.KafkaConfig
import dev.gamov.flightdemo.ktor.db.FlightRepository
import dev.gamov.flightdemo.ktor.service.DatabaseService
import dev.gamov.flightdemo.ktor.service.KafkaService
import org.koin.dsl.module

/**
 * Koin dependency injection module for the application
 */
val appModule = module {
    // Configuration
    single { KafkaConfig.load() }
    single { DatabaseConfig.load() }

    // Services
    single { KafkaService(get()) }
    single { DatabaseService(get()) }

    // Repositories
    single { FlightRepository(get()) }
}
