package dev.gamov.flightdemo.ktor

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.plugins.cors.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import dev.gamov.flightdemo.ktor.routes.configureSseRoutes
import dev.gamov.flightdemo.ktor.routes.configureRestRoutes
import dev.gamov.flightdemo.ktor.service.KafkaService
import org.koin.ktor.ext.inject
import kotlinx.serialization.json.Json
import org.koin.core.logger.Level as KoinLevel
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import org.slf4j.LoggerFactory
import org.slf4j.event.Level

fun main() {
    embeddedServer(
        Netty,
        port = System.getenv("PORT")?.toIntOrNull() ?: 9080,
        host = System.getenv("HOST") ?: "0.0.0.0",
        module = Application::module
    ).start(wait = true)
}

fun Application.module() {
    val logger = LoggerFactory.getLogger("Application")
    logger.info("Starting Flight Tracking Demo Ktor Backend")

    // Install Koin for dependency injection
    install(Koin) {
        slf4jLogger(level = KoinLevel.INFO)
        modules(appModule)
    }

    // Configure content negotiation with JSON
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }

    // Configure CORS
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowHeader(HttpHeaders.AccessControlAllowOrigin)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Accept)
        anyHost() // In production, you should restrict this
    }

    // Configure logging
    install(CallLogging) {
        level = Level.INFO
    }

    // Configure default headers
    install(DefaultHeaders) {
        header("X-Engine", "Ktor")
        header(HttpHeaders.CacheControl, "no-cache, no-store, must-revalidate")
        header(HttpHeaders.Pragma, "no-cache")
        header(HttpHeaders.Expires, "0")
    }

    // Configure status pages for error handling
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            logger.error("Unhandled exception", cause)
            call.respondText(
                text = "Internal server error: ${cause.message}",
                contentType = ContentType.Text.Plain,
                status = HttpStatusCode.InternalServerError
            )
        }
    }

    // Configure compression
    install(Compression) {
        gzip {
            priority = 1.0
        }
        deflate {
            priority = 10.0
            minimumSize(1024)
        }
    }

    // Get the KafkaService from Koin
    val kafkaService by inject<KafkaService>()

    // Start the KafkaService
    kafkaService.start()

    // Configure SSE routes
    configureSseRoutes(kafkaService)

    // Configure REST routes
    configureRestRoutes()

    // Configure routing
    routing {
        get("/") {
            call.respondText("Flight Tracking Demo API is running!", ContentType.Text.Plain)
        }
    }

    // Add shutdown hook to stop the KafkaService
    environment.monitor.subscribe(ApplicationStopping) {
        logger.info("Stopping KafkaService")
        kafkaService.stop()
    }

    logger.info("Flight Tracking Demo Ktor Backend started")
}
