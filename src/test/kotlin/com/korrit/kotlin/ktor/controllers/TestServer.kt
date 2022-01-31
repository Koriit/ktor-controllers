package com.korrit.kotlin.ktor.controllers

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.korrit.kotlin.ktor.convertTime
import com.korrit.kotlin.ktor.features.UUIDCallId
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.features.DataConversion
import io.ktor.http.ContentType.Application
import io.ktor.jackson.JacksonConverter
import io.ktor.routing.Routing
import io.ktor.routing.routing
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.testing.TestApplicationEngine

fun testServer(
    controller: Routing.() -> Unit
): TestApplicationEngine {
    return TestApplicationEngine(
        applicationEngineEnvironment {
            val jackson = JsonMapper.builder()
                .addModule(KotlinModule.Builder().build())
                .addModule(JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build()

            module {
                install(UUIDCallId)
                install(DataConversion) {
                    convertTime(jackson)
                }
                install(ContentNegotiation) {
                    register(Application.Json, JacksonConverter(jackson))
                }
                routing {
                    controller()
                }
            }
        }
    )
}
