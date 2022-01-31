// intended to differentiate from core functions
@file:Suppress("FunctionName")

package com.korrit.kotlin.ktor.controllers

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

/**
 * Ktor attribute key of request handler object provider.
 */
val InputKey = AttributeKey<() -> Input<*>>("Input Provider")

/**
 * Builds a route to match `GET` requests with specified [path] that delegates responding to provided handler.
 *
 * @param path route's path to match
 * @param handlerProvider lambda which returns request handler object, such design allows some freedom and API analysis
 * @param T Request handler
 */
@ContextDsl
inline fun <reified T : Input<*>> Route.GET(path: String, noinline handlerProvider: () -> T): Route {
    return get(path) {
        handlerProvider()(this)
    }.apply {
        attributes.put(InputKey, handlerProvider)
    }
}

/**
 * Builds a route to match `POST` requests with specified [path] that delegates responding to provided handler.
 *
 * @param path route's path to match
 * @param handlerProvider lambda which returns request handler object, such design allows some freedom and API analysis
 * @param T Request handler
 */
@ContextDsl
inline fun <reified T : Input<*>> Route.POST(path: String, noinline handlerProvider: () -> T): Route {
    return post(path) {
        handlerProvider()(this)
    }.apply {
        attributes.put(InputKey, handlerProvider)
    }
}

/**
 * Builds a route to match `PUT` requests with specified [path] that delegates responding to provided handler.
 *
 * @param path route's path to match
 * @param handlerProvider lambda which returns request handler object, such design allows some freedom and API analysis
 * @param T Request handler
 */
@ContextDsl
inline fun <reified T : Input<*>> Route.PUT(path: String, noinline handlerProvider: () -> T): Route {
    return put(path) {
        handlerProvider()(this)
    }.apply {
        attributes.put(InputKey, handlerProvider)
    }
}

/**
 * Builds a route to match `PATCH` requests with specified [path] that delegates responding to provided handler.
 *
 * @param path route's path to match
 * @param handlerProvider lambda which returns request handler object, such design allows some freedom and API analysis
 * @param T Request handler
 */
@ContextDsl
inline fun <reified T : Input<*>> Route.PATCH(path: String, noinline handlerProvider: () -> T): Route {
    return patch(path) {
        handlerProvider()(this)
    }.apply {
        attributes.put(InputKey, handlerProvider)
    }
}

/**
 * Builds a route to match `HEAD` requests with specified [path] that delegates responding to provided handler.
 *
 * @param path route's path to match
 * @param handlerProvider lambda which returns request handler object, such design allows some freedom and API analysis
 * @param T Request handler
 */
@ContextDsl
inline fun <reified T : Input<*>> Route.HEAD(path: String, noinline handlerProvider: () -> T): Route {
    return head(path) {
        handlerProvider()(this)
    }.apply {
        attributes.put(InputKey, handlerProvider)
    }
}

/**
 * Builds a route to match `DELETE` requests with specified [path] that delegates responding to provided handler.
 *
 * @param path route's path to match
 * @param handlerProvider lambda which returns request handler object, such design allows some freedom and API analysis
 * @param T Request handler
 */
@ContextDsl
inline fun <reified T : Input<*>> Route.DELETE(path: String, noinline handlerProvider: () -> T): Route {
    return delete(path) {
        handlerProvider()(this)
    }.apply {
        attributes.put(InputKey, handlerProvider)
    }
}

/**
 * Builds a route to match `OPTIONS` requests with specified [path] that delegates responding to provided handler.
 *
 * @param path route's path to match
 * @param handlerProvider lambda which returns request handler object, such design allows some freedom and API analysis
 * @param T Request handler
 */
@ContextDsl
inline fun <reified T : Input<*>> Route.OPTIONS(path: String, noinline handlerProvider: () -> T): Route {
    return options(path) {
        handlerProvider()(this)
    }.apply {
        attributes.put(InputKey, handlerProvider)
    }
}
