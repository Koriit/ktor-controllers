package korrit.kotlin.ktor.controllers

import io.ktor.server.engine.ApplicationEngine
import java.util.concurrent.CyclicBarrier

/**
 * Helper function to fully start your server in a blocking way.
 *
 * WARN: if your server fails to start - and doesn't shutdown the JVM - this function will block forever
 */
fun ApplicationEngine.fullStart() {
    val started = CyclicBarrier(2)
    environment.monitor.subscribe(io.ktor.application.ApplicationStarted) {
        started.await()
    }
    start(wait = false)
    started.await()
}
