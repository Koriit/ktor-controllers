package korrit.kotlin.ktor.controllers.exceptions

import io.ktor.features.BadRequestException

/**
 * General exception related to input.
 */
open class InputException(message: String, cause: Throwable? = null) : BadRequestException(message, cause)
