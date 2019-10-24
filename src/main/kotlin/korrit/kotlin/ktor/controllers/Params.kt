package korrit.kotlin.ktor.controllers

import io.ktor.util.KtorExperimentalAPI
import korrit.kotlin.ktor.controllers.delegates.HeaderParamDelegate
import korrit.kotlin.ktor.controllers.delegates.PathParamDelegate
import korrit.kotlin.ktor.controllers.delegates.QueryParamDelegate
import kotlin.reflect.KProperty


interface DelegateProvider<out R> {
    operator fun provideDelegate(thisRef: Input<*>, property: KProperty<*>): R
}

@KtorExperimentalAPI
inline fun <reified T> Input<*>.path(name: String? = null) = object : DelegateProvider<PathParamDelegate<T>> {
    override operator fun provideDelegate(thisRef: Input<*>, property: KProperty<*>) = PathParamDelegate<T>(name ?: property.name, T::class.java, thisRef)
}

@KtorExperimentalAPI
inline fun <reified T> Input<*>.path(name: String? = null, default: T) = object : DelegateProvider<PathParamDelegate<T>> {
    override operator fun provideDelegate(thisRef: Input<*>, property: KProperty<*>) = PathParamDelegate(name ?: property.name, T::class.java, thisRef, default)
}

@KtorExperimentalAPI
inline fun <reified T> Input<*>.query(name: String? = null) = object : DelegateProvider<QueryParamDelegate<T>> {
    override operator fun provideDelegate(thisRef: Input<*>, property: KProperty<*>) = QueryParamDelegate<T>(name ?: property.name, T::class.java, thisRef)
}

@KtorExperimentalAPI
inline fun <reified T> Input<*>.query(name: String? = null, default: T) = object : DelegateProvider<QueryParamDelegate<T>> {
    override operator fun provideDelegate(thisRef: Input<*>, property: KProperty<*>) = QueryParamDelegate(name ?: property.name, T::class.java, thisRef, default)
}

@KtorExperimentalAPI
inline fun <reified T> Input<*>.header(name: String) = HeaderParamDelegate<T>(name, T::class.java, this)

@KtorExperimentalAPI
inline fun <reified T> Input<*>.header(name: String, default: T) = HeaderParamDelegate(name, T::class.java, default, this)

