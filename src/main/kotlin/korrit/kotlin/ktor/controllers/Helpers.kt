package korrit.kotlin.ktor.controllers

import io.ktor.server.engine.ApplicationEngine
import java.util.concurrent.CyclicBarrier

fun ApplicationEngine.fullStart() {
    val started = CyclicBarrier(2)
    environment.monitor.subscribe(io.ktor.application.ApplicationStarted) {
        started.await()
    }
    start(wait = false)
    started.await()
}