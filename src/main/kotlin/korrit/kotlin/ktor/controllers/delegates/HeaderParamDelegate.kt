package korrit.kotlin.ktor.controllers.delegates

import io.ktor.features.conversionService
import io.ktor.util.KtorExperimentalAPI
import java.lang.reflect.Type
import korrit.kotlin.ktor.controllers.Input
import korrit.kotlin.ktor.controllers.exceptions.InputException
import kotlin.reflect.KProperty

/**
 * Delegate to retrieve parameters from request headers.
 *
 * @param T type of the input body
 * @param name header name
 * @param type return type of the parameter, must hold _T_ class
 * @param receiver internal reference to the delegate receiver
 */
@KtorExperimentalAPI
class HeaderParamDelegate<T : Any?>(
    val name: String,
    val type: Type,
    private val receiver: Input<*>
) {

    /**
     * Parameters default value. Throws if no default.
     */
    // lateinit and Delegates.notNull() do not support nullable types, hence provider design used here
    val default by lazy { defaultValueProvider() }

    private var defaultValueProvider: () -> T = { throw InputException("Missing $name header") }

    /**
     * Whether parameter is required. This is inferred with presence of default value.
     */
    var required = true
        private set

    /**
     * Delegate to retrieve parameters from request headers.
     *
     * @param T type of the input body
     * @param name header name
     * @param type return type of the parameter, must hold _T_ class
     * @param default default value in case parameter was not provided, implies parameter is not required
     * @param receiver internal reference to the delegate receiver
     */
    constructor(name: String, type: Type, default: T, receiver: Input<*>) : this(name, type, receiver) {
        this.defaultValueProvider = { default }
        this.required = false
    }

    private val value: T by lazy {
        val values = receiver.call.request.headers.getAll(name)
        if (values == null || values.isEmpty()) {
            return@lazy default
        }

        @Suppress("TooGenericExceptionCaught", "UNCHECKED_CAST") // intended
        try {
            receiver.call.application.conversionService.fromValues(values, type) as T
        } catch (e: Exception) {
            throw InputException("Cannot parse $name header: ${e.message}", e)
        }
    }

    /**
     * Function called on delegated property access.
     * https://kotlinlang.org/docs/reference/delegated-properties.html
     */
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T = value
}
