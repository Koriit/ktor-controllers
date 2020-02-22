package korrit.kotlin.ktor.controllers

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.features.UnsupportedMediaTypeException
import io.ktor.http.ContentType
import io.ktor.request.contentType
import io.ktor.request.receive
import io.ktor.util.pipeline.PipelineContext

/** Shorthand alias of [PipelineContext]. */
typealias Ctx = PipelineContext<Unit, ApplicationCall>

/**
 * Base class for request handlers. Many controller helpers depend on it.
 *
 * There are 3 goals:
 * 1. Ease creation of feature-full HTTP APIs
 * 2. Allow automatic analysis of your API without analyzing code
 * 3. Reduce usage of annotation to *zero*
 *
 * @param contentType content type of the request, null implies default which should be handler by the user
 * @param strict whether to apply strict validations
 * @param deprecated whether this API is deprecated
 * @param bodyRequired whether request body is required
 */
// we are prefixing variables with _ because we don't want to limit possible property names of Input derivatives
// and we still want them to be accessible by analyzers
@Suppress("LocalVariableName", "PropertyName", "VariableNaming")
abstract class Input<BodyType : Any>(
    contentType: ContentType? = null,
    strict: Boolean = false,
    deprecated: Boolean = false,
    bodyRequired: Boolean = true
) {
    /**
     * Whether this API is deprecated.
     */
    val _deprecated = deprecated

    /**
     * Whether request body is required.
     */
    val _bodyRequired = bodyRequired

    /**
     * Content type of the request. Null implies default which should be handler by the user.
     */
    val _contentType = contentType

    /**
     * Whether to apply strict validations.
     */
    val _strict = strict

    /**
     * Internal reference to the `call`, used in various controllers functions.
     */
    internal lateinit var call: ApplicationCall
        private set

    /**
     * Request handler.
     */
    abstract suspend fun Ctx.respond()

    /**
     * Contained way of setting `call` and calling `respond`.
     */
    internal suspend operator fun invoke(ctx: PipelineContext<Unit, ApplicationCall>) {
        call = ctx.call
        ctx.respond()
    }

    /**
     * Typed way to receive request body.
     *
     * @throws UnsupportedMediaTypeException if `strict` is `true` and request's content type doesn't match `contentType`
     */
    suspend inline fun <reified R : BodyType> PipelineContext<Unit, ApplicationCall>.body(): R {
        if (_contentType != null && _strict && !call.request.contentType().match(_contentType)) {
            throw UnsupportedMediaTypeException(call.request.contentType())
        }

        return call.receive()
    }
}

/**
 * Shorthand and explicit way to declare request with no body.
 */
abstract class EmptyBodyInput : Input<Nothing>(
    bodyRequired = false
)
