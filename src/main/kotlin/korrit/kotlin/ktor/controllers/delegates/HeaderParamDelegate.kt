package korrit.kotlin.ktor.controllers.delegates

import io.ktor.features.BadRequestException
import io.ktor.features.conversionService
import io.ktor.util.KtorExperimentalAPI
import korrit.kotlin.ktor.controllers.Input
import java.lang.reflect.Type
import kotlin.reflect.KProperty

@KtorExperimentalAPI
class HeaderParamDelegate<T : Any?>(
        val name: String,
        val type: Type,
        private val receiver: Input<*>
) {

    // lateinit and Delegates.notNull() do not support nullable types, hence provider design
    val default by lazy { defaultProvider() }

    var required = true
        private set

    private var defaultProvider: () -> T = { throw BadRequestException("Missing $name header") }

    constructor(name: String, type: Type, default: T, receiver: Input<*>) : this(name, type, receiver) {
        this.defaultProvider = { default }
        this.required = false
    }

    private val value: T by lazy {
        val values = receiver.call.request.headers.getAll(name)
        if (values == null || values.isEmpty()) {
            return@lazy default
        }

        @Suppress("UNCHECKED_CAST")
        receiver.call.application.conversionService.fromValues(values, type) as T
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T = value

}