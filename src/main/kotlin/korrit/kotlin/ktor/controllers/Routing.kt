package korrit.kotlin.ktor.controllers

import io.ktor.routing.Route
import io.ktor.routing.delete
import io.ktor.routing.get
import io.ktor.routing.head
import io.ktor.routing.options
import io.ktor.routing.patch
import io.ktor.routing.post
import io.ktor.routing.put
import io.ktor.util.AttributeKey
import io.ktor.util.pipeline.ContextDsl

val InputKey = AttributeKey<() -> Input<*>>("Input Provider")

@Suppress("FunctionName")
@ContextDsl
inline fun <reified T : Input<*>> Route.GET(path: String, noinline provider: () -> T): Route {
    val route = get(path) {
        val request = provider()
        request(this)
    }
    route.attributes.put(InputKey, provider)
    return route
}

@Suppress("FunctionName")
@ContextDsl
inline fun <reified T : Input<*>> Route.POST(path: String, noinline provider: () -> T): Route {
    val route = post(path) {
        val request = provider()
        request(this)
    }
    route.attributes.put(InputKey, provider)
    return route
}

@Suppress("FunctionName")
@ContextDsl
inline fun <reified T : Input<*>> Route.PUT(path: String, noinline provider: () -> T): Route {
    val route = put(path) {
        val request = provider()
        request(this)
    }
    route.attributes.put(InputKey, provider)
    return route
}

@Suppress("FunctionName")
@ContextDsl
inline fun <reified T : Input<*>> Route.PATCH(path: String, noinline provider: () -> T): Route {
    val route = patch(path) {
        val request = provider()
        request(this)
    }
    route.attributes.put(InputKey, provider)
    return route
}

@Suppress("FunctionName")
@ContextDsl
inline fun <reified T : Input<*>> Route.HEAD(path: String, noinline provider: () -> T): Route {
    val route = head(path) {
        val request = provider()
        request(this)
    }
    route.attributes.put(InputKey, provider)
    return route
}

@Suppress("FunctionName")
@ContextDsl
inline fun <reified T : Input<*>> Route.DELETE(path: String, noinline provider: () -> T): Route {
    val route = delete(path) {
        val request = provider()
        request(this)
    }
    route.attributes.put(InputKey, provider)
    return route
}

@Suppress("FunctionName")
@ContextDsl
inline fun <reified T : Input<*>> Route.OPTIONS(path: String, noinline provider: () -> T): Route {
    val route = options(path) {
        val request = provider()
        request(this)
    }
    route.attributes.put(InputKey, provider)
    return route
}