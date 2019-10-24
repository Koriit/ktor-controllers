package korrit.kotlin.ktor.controllers

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.features.BadRequestException
import io.ktor.features.UnsupportedMediaTypeException
import io.ktor.features.conversionService
import io.ktor.http.ContentType
import io.ktor.request.contentType
import io.ktor.request.receive
import io.ktor.util.pipeline.PipelineContext
import java.lang.reflect.Type
import kotlin.reflect.KProperty

@Suppress("LocalVariableName", "PropertyName")
abstract class Input<BodyType : Any>(
        contentType: ContentType? = null,
        strict: Boolean = false,
        deprecated: Boolean = false,
        bodyRequired: Boolean = true
) {

    val _deprecated = deprecated
    val _bodyRequired = bodyRequired
    val _contentType = contentType

    val _strict = strict

    internal lateinit var call: ApplicationCall
        private set

    abstract suspend fun PipelineContext<Unit, ApplicationCall>.respond()

    suspend operator fun invoke(ctx: PipelineContext<Unit, ApplicationCall>) {
        call = ctx.call
        ctx.respond()
    }

    suspend inline fun <reified R : BodyType> PipelineContext<Unit, ApplicationCall>.body(): R {
        if (_contentType != null && _strict && !_contentType.match(call.request.contentType())) {
            throw UnsupportedMediaTypeException(call.request.contentType())
        }

        return call.receive()
    }
}

abstract class EmptyBodyInput : Input<Nothing>(
        bodyRequired = false
)

