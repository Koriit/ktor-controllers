package korrit.kotlin.ktor.controllers

import io.ktor.server.engine.ApplicationEngine
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.runBlocking

/**
 * Helper function to fully start your server in a blocking way.
 *
 * WARN: if your server fails to start - and doesn't shutdown the JVM - this function will block forever
 */
// TODO it may be that this function is not needed anymore after upgrading to Ktor 1.6
fun ApplicationEngine.fullStart() {
    val started = Channel<Unit>(1)
    environment.monitor.subscribe(io.ktor.application.ApplicationStarted) {
        started.trySendBlocking(Unit)
    }
    start(wait = false)
    runBlocking {
        started.receive()
    }
}
