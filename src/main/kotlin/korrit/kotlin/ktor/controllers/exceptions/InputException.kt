package korrit.kotlin.ktor.controllers.exceptions

import io.ktor.features.BadRequestException
import io.ktor.util.KtorExperimentalAPI

/**
 * General exception related to input.
 */
@KtorExperimentalAPI
open class InputException(message: String, cause: Throwable? = null) : BadRequestException(message, cause)
