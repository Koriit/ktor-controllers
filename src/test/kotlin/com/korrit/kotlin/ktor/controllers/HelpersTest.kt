package com.korrit.kotlin.ktor.controllers

import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import org.junit.jupiter.api.Test

internal class HelpersTest {

    @Test
    fun `Should full start server`() {
        val server = embeddedServer(CIO, port = 12345) {}

        server.fullStart()

        server.stop(0, 0)
    }
}
