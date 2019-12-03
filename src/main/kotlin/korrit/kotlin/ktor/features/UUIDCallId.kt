package korrit.kotlin.ktor.features

import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.ApplicationFeature
import io.ktor.features.CallId
import io.ktor.http.HttpHeaders
import java.util.UUID

/**
 * This is [CallId] feature with predefined configuration.
 *
 * Call id header: X-Request-ID
 * Call id generation: UUID
 * Call id required: yes
 */
class UUIDCallId {

    /**
     * Feature installation object.
     */
    companion object Feature : ApplicationFeature<ApplicationCallPipeline, Unit, CallId> {

        override val key = CallId.key

        override fun install(pipeline: ApplicationCallPipeline, configure: Unit.() -> Unit): CallId {
            return CallId.install(pipeline) {
                header(HttpHeaders.XRequestId)
                generate { UUID.randomUUID().toString() }
                verify { it.isNotBlank() }
            }
        }
    }
}
