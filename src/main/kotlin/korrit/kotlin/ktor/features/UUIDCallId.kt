package korrit.kotlin.ktor.features

import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.ApplicationFeature
import io.ktor.features.CallId
import io.ktor.http.HttpHeaders
import java.util.UUID

/**
 * This is [CallId] feature with predefined configuration.
 *
 * Call id header: X-Correlation-ID by default
 * Call id generation: UUID
 * Call id required: yes
 */
class UUIDCallId {

    /** Configuration of UUIDCallId feature. */
    class Config {
        /** Configures which header should be checked for Call Id value. */
        var primaryHeader: String = HttpHeaders.XCorrelationId

        var secondaryHeader: String = HttpHeaders.XRequestId
    }

    /**
     * Feature installation object.
     */
    companion object Feature : ApplicationFeature<ApplicationCallPipeline, Config, CallId> {

        override val key = CallId.key

        override fun install(pipeline: ApplicationCallPipeline, configure: Config.() -> Unit): CallId {
            val config = Config()
            config.configure()

            return CallId.install(pipeline) {
                header(config.primaryHeader)
                header(config.secondaryHeader)
                generate { UUID.randomUUID().toString() }
                verify { it.isNotBlank() }
            }
        }
    }
}
