package korrit.kotlin.ktor.controllers.patch

import io.ktor.util.KtorExperimentalAPI
import korrit.kotlin.ktor.controllers.exceptions.InputException
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1

/**
 * Base class for delegates of patch properties.
 *
 * This class holds information on both **patch** property and **patched** property.
 * Do not confuse the both.
 */
sealed class AbstractPatchDelegate<T : Any?, P : Any?> {
    /** Name of *patch* property. */
    abstract val name: String
    /** Whether *patch* property is required. */
    abstract val isRequired: Boolean
    /** Whether *patch* value has been provided. */
    abstract val isSet: Boolean
    /** Whether *patched* property is nullable. */
    abstract val isNullable: Boolean

    /** Reference to *patched* property definition. */
    internal abstract val prop: KProperty1<T, P>
    /** Shorthand reference to mutable *patched* property definition. Throws if property is read-only. */
    internal val mutableProp: KMutableProperty1<T, P> get() = prop as KMutableProperty1<T, P>
}

/**
 * Delegate for flat values: scalars and objects used as-is.
 */
@KtorExperimentalAPI
class PatchDelegate<T : Any?, P : Any?>(
    override val name: String,
    override val isRequired: Boolean,
    override val prop: KProperty1<T, P>
) : ReadWriteProperty<Any?, P>, AbstractPatchDelegate<T, P>() {

    /** The actual value that is held by this delegate. */
    internal var value: P? = null
        private set

    override var isSet: Boolean = false
        private set

    override val isNullable = prop.returnType.isMarkedNullable

    override operator fun getValue(thisRef: Any?, property: KProperty<*>): P {
        // when directly accessing not set field we must throw an exception
        if (!isSet) throw InputException("Value is missing")

        @Suppress("UNCHECKED_CAST") // this is safe as it happens after calling setValue with P
        return value as P
    }

    override operator fun setValue(thisRef: Any?, property: KProperty<*>, value: P) {
        this.value = value
        this.isSet = true
    }
}

/**
 * Delegate for nested patches.
 */
@KtorExperimentalAPI
class NestedPatchDelegate<T : Any?, P : Any?, N : PatchOf<P>>(
    override val name: String,
    override val isRequired: Boolean,
    override val prop: KProperty1<T, P>
) : ReadWriteProperty<Any?, N>, AbstractPatchDelegate<T, P>() {

    /** Nested patch. */
    internal var patch: N? = null
        private set

    override var isSet: Boolean = false
        private set

    override val isNullable = prop.returnType.isMarkedNullable

    override operator fun getValue(thisRef: Any?, property: KProperty<*>): N {
        // when directly accessing not set field we must throw an exception
        if (!isSet) throw InputException("Value is missing")

        @Suppress("UNCHECKED_CAST") // this is safe as it happens after calling setValue with P
        return patch as N
    }

    override operator fun setValue(thisRef: Any?, property: KProperty<*>, value: N) {
        this.patch = value
        this.isSet = true
    }
}
