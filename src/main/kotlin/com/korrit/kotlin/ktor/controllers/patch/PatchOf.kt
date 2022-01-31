package com.korrit.kotlin.ktor.controllers.patch

import com.korrit.kotlin.ktor.controllers.exceptions.InputException
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.full.instanceParameter
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.primaryConstructor

/**
 * Base class that allows defining patch request bodies with delegates.
 *
 * **patch** and **patched** functions follow *PATCH* semantics.
 *
 * **update** and **updated** functions follow *PUT* semantics.
 *
 * This is semi-compatible with RFC7396 as there are constraints imposed by type system
 * which are not considered by this rfc because it is defined on generic JSON.
 * RFC7396 is also kind of broken by required constraint that can be forced on patch property.
 *
 * There are no public fields as they would limit possible patch property names.
 *
 * @param T Target patched class
 */
open class PatchOf<T : Any?> {

    companion object {
        /** Cache of copy function and mapping of patch delegates to copy params. */
        private val copyCache = ConcurrentHashMap<KClass<*>, Pair<KFunction<*>, Map<KProperty1<*, *>, KParameter>>>()

        /** Cache of primary constructor and mapping of patch delegates to constructor params. */
        private val constructorCache = ConcurrentHashMap<KClass<*>, Pair<KFunction<*>, Map<KProperty1<*, *>, KParameter>>>()
    }

    /**
     * Internal reference to class object of patched class.
     *
     * We must use star projection because T must be nullable and KClass<*> does not accept T?
     */
    private lateinit var patchedClass: KClass<*>

    /**
     * Whether in-place modifications are possible for this patch object.
     */
    private var canModifyInPlace = true

    /**
     * List of delegates defined in this patch object.
     */
    private val delegates = mutableListOf<AbstractPatchDelegate<T, Any?>>()

    /**
     * Helper function to create required patch delegates.
     *
     * Patch value overrides entirely the patched property.
     *
     * @param prop Definition of patched property, i.e. PatchedClass::patchedProperty
     * @param required Must be true. Additional param allows us to have an overload that declares different return type
     * @param D Target patched class. Hacky [T] redeclaration to get it reified
     * @param P Type of patched property
     */
    inline fun <reified D : T, P : Any?> patchOf(prop: KProperty1<D, P>, required: Boolean): RequiredPatchDelegateProvider<P> {
        assert(required) { "Parameter 'required' of 'patchOf' delegate builder must be true or omitted" }
        return RequiredPatchDelegateProvider(prop, D::class)
    }

    /**
     * Helper function to create patch delegates.
     *
     * Patch value overrides entirely the patched property.
     *
     * @param prop Definition of patched property, i.e. PatchedClass::patchedProperty
     * @param D Target patched class. Hacky [T] redeclaration to get it reified
     * @param P Type of patched property
     */
    inline fun <reified D : T, P : Any?> patchOf(prop: KProperty1<D, P>): PatchDelegateProvider<P> {
        return PatchDelegateProvider(prop, D::class)
    }

    /**
     * Helper function to create required nested patch delegates.
     *
     * Patch value updates patched property with the same logic as is applied to the patched object.
     *
     * @param prop Definition of patched property, i.e. PatchedClass::nestedObject (which is of type NestedClass)
     * @param by The patch class to be used for this property, i.e. NestedClassPatch::class (which is PatchOf<NestedClass>)
     * @param required Must be true. Additional param allows us to have an overload that declares different return type
     * @param D Target patched class. Hacky [T] redeclaration to get it reified
     * @param P Type of patched property
     * @param U Type of Patch Class that is going to patch property
     */
    @Suppress("UNUSED_PARAMETER") // param `by` allows the compiler to infer generic type U
    inline fun <reified D : T, P : Any?, U : PatchOf<P>> patchOf(prop: KProperty1<D, P>, by: KClass<U>, required: Boolean): RequiredNestedPatchDelegateProvider<P, U> {
        assert(required) { "Parameter 'required' of 'patchOf' delegate builder must be true or omitted" }
        return RequiredNestedPatchDelegateProvider(prop, D::class)
    }

    /**
     * Helper function to create nested patch delegates.
     *
     * Patch value updates patched property with the same logic as is applied to the patched object.
     *
     * @param prop Definition of patched property, i.e. PatchedClass::nestedObject (which is of type NestedClass)
     * @param by The patch class to be used for this property, i.e. NestedClassPatch::class (which is PatchOf<NestedClass>)
     * @param D Target patched class. Hacky [T] redeclaration to get it reified
     * @param P Type of patched property
     * @param U Type of Patch Class that is going to patch property
     */
    @Suppress("UNUSED_PARAMETER") // param `by` allows the compiler to infer generic type U
    inline fun <reified D : T, P : Any?, U : PatchOf<P>> patchOf(prop: KProperty1<D, P>, by: KClass<U>): NestedPatchDelegateProvider<P, U> {
        return NestedPatchDelegateProvider(prop, D::class)
    }

    /**
     * Creates new instance of patched class.
     *
     * There are following constraints:
     * 1. Patched class must have a primary constructor
     * 2. There are no delegates targeting properties outside of primary constructor
     * 3. All non-optional parameters of primary constructor are mapped to delegates
     * 4. Values are present for all required, non-optional and non-nullable properties
     *
     * @see getConstructor
     */
    open fun instance(): T {
        if (!::patchedClass.isInitialized) throw UnsupportedOperationException("${javaClass.simpleName} does not define any patch delegate")

        val (ctor, ctorParams) = getConstructor()

        val params: MutableMap<KParameter, Any?> = mutableMapOf()

        for (patch in delegates) {
            val param = ctorParams.getValue(patch.patchedProp)
            if (!patch.isSet) {
                if (patch.isRequired) throw InputException("Missing field: ${patch.name}")
                // missing but optional so we can skip it
                if (param.isOptional) continue
                // missing nullable, non-optional fields are implicitly considered null
                if (!patch.isNullable) throw InputException("Missing field: ${patch.name}")
            }

            params[param] = when (patch) {
                is PatchDelegate<T, Any?> -> patch.value
                is NestedPatchDelegate<T, *, *> -> patch.patch!!.instance()
            }
        }

        @Suppress("UNCHECKED_CAST") // safe
        return ctor.callBy(params) as T
    }

    /**
     * Modifies object in-place with *PATCH* semantics.
     *
     * Requires all delegates to target mutable properties(defined with **var**).
     * This also applies to all nested patches.
     *
     * It uses [instance] function internally, so, the same constraints apply if
     * some nested object is null on the patched object.
     *
     * Exception is thrown if some patch value is required but is missing.
     *
     * @param obj Object to patch
     */
    open fun patch(obj: T) {
        if (!::patchedClass.isInitialized) throw UnsupportedOperationException("${javaClass.simpleName} does not define any patch delegate")
        if (obj == null) throw IllegalArgumentException("Patched object cannot be null")
        if (!canModifyInPlace) throw UnsupportedOperationException("This operation is only supported for patches of mutable properties")

        for (patch in delegates) {
            if (!patch.isSet) {
                if (patch.isRequired) throw InputException("Missing field: ${patch.name}")
                continue
            }

            val value = when (patch) {
                is PatchDelegate<T, Any?> -> {
                    patch.value
                }
                is NestedPatchDelegate<T, *, *> -> {
                    @Suppress("UNCHECKED_CAST") // safe cast
                    patch as NestedPatchDelegate<T, Any?, PatchOf<Any?>>

                    val nestedPatch = patch.patch!!
                    val nestedValue = patch.patchedProp.get(obj)

                    if (nestedValue != null) {
                        nestedPatch.patch(nestedValue)
                        nestedValue
                    } else {
                        nestedPatch.instance()
                    }
                }
            }

            patch.mutableProp.set(obj, value)
        }
    }

    /**
     * Returns new copy of object, modified with *PATCH* semantics.
     * Original object is not modified.
     *
     * This does not require all patched properties to be mutable(**var**) like [patch].
     *
     * It uses [instance] function internally, so, the same constraints apply if
     * some nested object is null on the patched object.
     *
     * Additionally it only supports data classes.
     * @see getCopy
     *
     * Exception is thrown if some patch value is required but is missing.
     *
     * @param obj Object to be patched
     */
    open fun patched(obj: T): T {
        if (!::patchedClass.isInitialized) throw UnsupportedOperationException("${javaClass.simpleName} does not define any patch delegate")
        if (obj == null) throw IllegalArgumentException("Patched object cannot be null")

        val (copy, copyParams) = getCopy()

        val params: MutableMap<KParameter, Any?> = mutableMapOf(
            copy.instanceParameter!! to obj
        )

        for (patch in delegates) {
            if (!patch.isSet) {
                if (patch.isRequired) throw InputException("Missing field: ${patch.name}")
                continue
            }
            val param = copyParams.getValue(patch.patchedProp)

            val value = when (patch) {
                is PatchDelegate<T, Any?> -> {
                    patch.value
                }
                is NestedPatchDelegate<T, *, *> -> {
                    @Suppress("UNCHECKED_CAST") // safe cast
                    patch as NestedPatchDelegate<T, Any?, PatchOf<Any?>>

                    val nestedPatch = patch.patch!!
                    val nestedValue = patch.patchedProp.get(obj)

                    if (nestedValue != null) {
                        nestedPatch.patched(nestedValue)
                    } else {
                        nestedPatch.instance()
                    }
                }
            }

            params[param] = value
        }

        @Suppress("UNCHECKED_CAST") // safe
        return copy.callBy(params) as T
    }

    /**
     * Modifies object in-place with *PUT* semantics.
     *
     * Requires all delegates to target mutable properties(defined with **var**).
     * This also applies to all nested patches.
     *
     * It uses [instance] function internally, so, the same constraints apply if
     * some nested object is null on the patched object.
     *
     * Exception is thrown if some patch value is non-nullable or required but is missing.
     *
     * @param obj Object to update
     */
    open fun update(obj: T) {
        if (!::patchedClass.isInitialized) throw UnsupportedOperationException("${javaClass.simpleName} does not define any patch delegate")
        if (obj == null) throw IllegalArgumentException("Updated object cannot be null")
        if (!canModifyInPlace) throw UnsupportedOperationException("This operation is only supported for updates of mutable properties")

        for (update in delegates) {
            if (!update.isSet) {
                if (!update.isNullable || update.isRequired) throw InputException("Missing field: ${update.name}")
                // missing nullable fields are implicitly considered null
                update.mutableProp.set(obj, null)
                continue
            }

            val value = when (update) {
                is PatchDelegate<T, Any?> -> {
                    update.value
                }
                is NestedPatchDelegate<T, *, *> -> {
                    @Suppress("UNCHECKED_CAST") // safe cast
                    update as NestedPatchDelegate<T, Any?, PatchOf<Any?>>

                    val nestedUpdate = update.patch!!
                    val nestedValue = update.patchedProp.get(obj)

                    if (nestedValue != null) {
                        nestedUpdate.update(nestedValue)
                        nestedValue
                    } else {
                        nestedUpdate.instance()
                    }
                }
            }

            update.mutableProp.set(obj, value)
        }
    }

    /**
     * Returns new copy of object, modified with *PUT* semantics.
     * Original object is not modified.
     *
     * This does not require all patched properties to be mutable(**var**) like [update].
     *
     * It uses [instance] function internally, so, the same constraints might apply if
     * some nested object is null on the patched object.
     *
     * Additionally it only supports data classes.
     * @see getCopy
     *
     * Exception is thrown if some patch value is non-nullable or required but is missing.
     *
     * @param obj Object to be patched
     */
    open fun updated(obj: T): T {
        if (!::patchedClass.isInitialized) throw UnsupportedOperationException("${javaClass.simpleName} does not define any patch delegate")
        if (obj == null) throw IllegalArgumentException("Updated object cannot be null")

        val (copy, copyParams) = getCopy()

        val params: MutableMap<KParameter, Any?> = mutableMapOf(
            copy.instanceParameter!! to obj
        )

        for (update in delegates) {
            val param = copyParams.getValue(update.patchedProp)

            if (!update.isSet) {
                if (!update.isNullable || update.isRequired) throw InputException("Missing field: ${update.name}")
                // missing nullable fields are implicitly considered null
                params[param] = null
                continue
            }

            val value = when (update) {
                is PatchDelegate<T, Any?> -> {
                    update.value
                }
                is NestedPatchDelegate<T, *, *> -> {
                    @Suppress("UNCHECKED_CAST") // safe cast
                    update as NestedPatchDelegate<T, Any?, PatchOf<Any?>>

                    val nestedUpdate = update.patch!!
                    val nestedValue = update.patchedProp.get(obj)

                    if (nestedValue != null) {
                        nestedUpdate.updated(nestedValue)
                    } else {
                        nestedUpdate.instance()
                    }
                }
            }

            params[param] = value
        }

        @Suppress("UNCHECKED_CAST") // safe
        return copy.callBy(params) as T
    }

    /**
     * Retrieves "copy constructor" of patched class and maps its parameters to defined delegates.
     *
     * Currently, only supports data classes as they have well defined standard copy function.
     *
     * Disallows delegates that target properties outside of primary constructor.
     */
    private fun getCopy() = copyCache.getOrPut(patchedClass) {
        if (!patchedClass.isData) throw UnsupportedOperationException("${patchedClass.simpleName} is not a data class")

        val copy = patchedClass.memberFunctions.first { it.name == "copy" }

        val params: Map<KProperty1<*, *>, KParameter> = delegates.map { delegate ->
            val param = copy.parameters.find { it.name == delegate.patchedProp.name }
                ?: throw UnsupportedOperationException("One of the patches targets property which is not defined in primary constructor")
            delegate.patchedProp to param
        }.toMap()

        copy to params
    }

    /**
     * Retrieves primary constructor of patched class and maps its parameters to defined delegates.
     *
     * All constructor parameters without default arguments must be matched, otherwise an exception is thrown.
     *
     * Disallows delegates that target properties outside of primary constructor.
     */
    private fun getConstructor() = constructorCache.getOrPut(patchedClass) {
        val ctor = patchedClass.primaryConstructor ?: throw UnsupportedOperationException("${patchedClass.simpleName} does not have primary constructor")

        val delegateProps = delegates.map { it.patchedProp.name }
        val ctorParams = ctor.parameters.mapNotNull { it.name }

        val unmapped = delegateProps - ctorParams
        if (unmapped.isNotEmpty()) throw UnsupportedOperationException("Patch class ${javaClass.simpleName} targets targets properties which are not defined in primary constructor: $unmapped")

        val params: Map<KProperty1<*, *>, KParameter> = ctor.parameters.mapNotNull { param ->
            val delegate = delegates.find { it.patchedProp.name == param.name }
            if (delegate == null) {
                // missing but optional so we can skip it
                if (param.isOptional) return@mapNotNull null
                else throw UnsupportedOperationException("Patch class ${javaClass.simpleName} is missing a delegate for ${param.name} constructor parameter")
            }
            delegate.patchedProp to param
        }.toMap()

        ctor to params
    }

    /**
     * Provides patch delegates.
     *
     * @param P Type of patched property
     * @property patchedProp Definition of patched property
     * @property patchedClazz Definition of patched class
     */
    inner class PatchDelegateProvider<P : Any?>(
        private val patchedProp: KProperty1<*, P>,
        private val patchedClazz: KClass<*>
    ) {

        /** Provides a delegate for given instance. */
        operator fun provideDelegate(thisRef: Any?, property: KProperty<*>): PatchDelegate<T, P> {
            if (::patchedClass.isInitialized && patchedClass != patchedClazz) throw IllegalArgumentException("All delegate targets must belong to ${patchedClass.simpleName} and '${property.name}' does not")
            patchedClass = patchedClazz

            if (patchedProp !is KMutableProperty1<*, *>) canModifyInPlace = false
            if (property !is KMutableProperty1<*, *>) throw UnsupportedOperationException("Delegated property must be mutable: ${property.name}")

            @Suppress("UNCHECKED_CAST") // Safe if not called from outside of this class
            val patch = PatchDelegate(property.name, patchedProp as KProperty1<T, P>)

            @Suppress("UNCHECKED_CAST") // Safe
            delegates.add(patch as AbstractPatchDelegate<T, Any?>)

            return patch
        }
    }

    /**
     * Provides required patch delegates.
     *
     * @param P Type of patched property
     * @property patchedProp Definition of patched property
     * @property patchedClazz Definition of patched class
     */
    inner class RequiredPatchDelegateProvider<P : Any?>(
        private val patchedProp: KProperty1<*, P>,
        private val patchedClazz: KClass<*>
    ) {

        /** Provides a delegate for given instance. */
        operator fun provideDelegate(thisRef: Any?, property: KProperty<*>): RequiredPatchDelegate<T, P> {
            if (::patchedClass.isInitialized && patchedClass != patchedClazz) throw IllegalArgumentException("All delegate targets must belong to ${patchedClass.simpleName} and '${property.name}' does not")
            patchedClass = patchedClazz

            if (patchedProp !is KMutableProperty1<*, *>) canModifyInPlace = false
            if (property !is KMutableProperty1<*, *>) throw UnsupportedOperationException("Delegated property must be mutable: ${property.name}")

            @Suppress("UNCHECKED_CAST") // Safe if not called from outside of this class
            val patch = RequiredPatchDelegate(property.name, patchedProp as KProperty1<T, P>)

            @Suppress("UNCHECKED_CAST") // Safe
            delegates.add(patch as AbstractPatchDelegate<T, Any?>)

            return patch
        }
    }

    /**
     * Provides nested patch delegates.
     *
     * @param P Type of patched property
     * @param U Type of Patch Class that is going to patch property
     * @property patchedProp Definition of patched property
     * @property patchedClazz Definition of patched class
     */
    inner class NestedPatchDelegateProvider<P : Any?, U : PatchOf<P>>(
        private val patchedProp: KProperty1<*, P>,
        private val patchedClazz: KClass<*>
    ) {
        /** Provides a delegate for given instance. */
        operator fun provideDelegate(thisRef: Any?, property: KProperty<*>): NestedPatchDelegate<T, P, U> {
            if (::patchedClass.isInitialized && patchedClass != patchedClazz) throw IllegalArgumentException("All delegate targets must belong to ${patchedClass.simpleName} and '${property.name}' does not")
            patchedClass = patchedClazz

            if (patchedProp !is KMutableProperty1<*, *>) canModifyInPlace = false
            if (property !is KMutableProperty1<*, *>) throw UnsupportedOperationException("Delegated property must be mutable: ${property.name}")

            @Suppress("UNCHECKED_CAST") // Safe if not called from outside of this class
            val patch = NestedPatchDelegate<T, P, U>(property.name, patchedProp as KProperty1<T, P>)

            @Suppress("UNCHECKED_CAST") // Safe
            delegates.add(patch as AbstractPatchDelegate<T, Any?>)

            return patch
        }
    }

    /**
     * Provides required nested patch delegates.
     *
     * @param P Type of patched property
     * @param U Type of Patch Class that is going to patch property
     * @property patchedProp Definition of patched property
     * @property patchedClazz Definition of patched class
     */
    inner class RequiredNestedPatchDelegateProvider<P : Any?, U : PatchOf<P>>(
        private val patchedProp: KProperty1<*, P>,
        private val patchedClazz: KClass<*>
    ) {
        /** Provides a delegate for given instance. */
        operator fun provideDelegate(thisRef: Any?, property: KProperty<*>): RequiredNestedPatchDelegate<T, P, U> {
            if (::patchedClass.isInitialized && patchedClass != patchedClazz) throw IllegalArgumentException("All delegate targets must belong to ${patchedClass.simpleName} and '${property.name}' does not")
            patchedClass = patchedClazz

            if (patchedProp !is KMutableProperty1<*, *>) canModifyInPlace = false
            if (property !is KMutableProperty1<*, *>) throw UnsupportedOperationException("Delegated property must be mutable: ${property.name}")

            @Suppress("UNCHECKED_CAST") // Safe if not called from outside of this class
            val patch = RequiredNestedPatchDelegate<T, P, U>(property.name, patchedProp as KProperty1<T, P>)

            @Suppress("UNCHECKED_CAST") // Safe
            delegates.add(patch as AbstractPatchDelegate<T, Any?>)

            return patch
        }
    }
}
