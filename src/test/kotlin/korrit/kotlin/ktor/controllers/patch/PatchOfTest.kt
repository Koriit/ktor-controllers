package korrit.kotlin.ktor.controllers.patch

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.client.utils.EmptyContent
import io.ktor.http.ContentType.Application
import io.ktor.http.HttpHeaders.ContentType
import io.ktor.http.HttpMethod.Companion.Patch
import io.ktor.http.HttpMethod.Companion.Put
import io.ktor.http.HttpStatusCode.Companion.NoContent
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.OutgoingContent.NoContent
import io.ktor.request.httpMethod
import io.ktor.response.respond
import io.ktor.server.testing.setBody
import koriit.kotlin.slf4j.logger
import korrit.kotlin.ktor.controllers.Ctx
import korrit.kotlin.ktor.controllers.Input
import korrit.kotlin.ktor.controllers.PATCH
import korrit.kotlin.ktor.controllers.PUT
import korrit.kotlin.ktor.controllers.exceptions.InputException
import korrit.kotlin.ktor.controllers.responds
import korrit.kotlin.ktor.controllers.testCases
import korrit.kotlin.ktor.controllers.testServer
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class PatchOfTest {

    data class NestedEntity(
        var booleanField: Boolean,
        var nestedField: NestedEntity? = null
    )

    data class Entity(
        var intField: Int,
        var stringField: String,
        var nullableField: String?,
        var floatField: Double?,
        var nestedField: NestedEntity?
    )

    class NestedPatch : PatchOf<NestedEntity?>() {
        var bf by patchOf(NestedEntity::booleanField)
        var nf by patchOf(NestedEntity::nestedField, by = NestedPatch::class)
    }

    class EntityPatch : PatchOf<Entity>() {
        var int by patchOf(Entity::intField)
        var string by patchOf(Entity::stringField)
        var float by patchOf(Entity::floatField)
        var nullable by patchOf(Entity::nullableField)
        var nested by patchOf(Entity::nestedField, by = NestedPatch::class)
    }

    class CustomPatch : PatchOf<Entity>() {
        var intField by patchOf(Entity::intField)
        var specialField : Boolean = false

        override fun patch(obj: Entity) {
            super.patch(obj)

            if(specialField) {
                obj.nullableField = null
            }
        }
    }

    private val log = logger {}
    private val jackson = ObjectMapper().registerKotlinModule()

    @Test
    fun `Test basic update cases`() {
        data class Case(
            val json: String,
            val expected: Entity
        )

        mapOf(
            "only required" to Case(
                json = """{ "int": 0, "string": "" }""",
                expected = Entity(
                    intField = 0,
                    stringField = "",
                    nullableField = null,
                    floatField = null,
                    nestedField = null
                )
            ),
            "nested update" to Case(
                json = "{\n  \"int\": 0, \n  \"string\": \"\",\n  \"nested\": {\n    \"bf\": true\n  }\n}",
                expected = Entity(
                    intField = 0,
                    stringField = "",
                    nullableField = null,
                    floatField = null,
                    nestedField = NestedEntity(true)
                )
            ),
            "nested instance creation" to Case(
                json = "{\n  \"int\": 0, \n  \"string\": \"\",\n  \"nested\": {\n    \"bf\":true,\n    \"nf\": {\n      \"bf\":true\n    }\n  }\n}",
                expected = Entity(
                    intField = 0,
                    stringField = "",
                    nullableField = null,
                    floatField = null,
                    nestedField = NestedEntity(
                        booleanField = true,
                        nestedField = NestedEntity(true)
                    )
                )
            ),
            "double nested instance creation" to Case(
                json = "{\n  \"int\": 0, \n  \"string\": \"\",\n  \"nested\": {\n    \"bf\":false,\n    \"nf\": {\n      \"bf\":true,\n      \"nf\": {\n        \"bf\":true\n      }\n    }\n  }\n}",
                expected = Entity(
                    intField = 0,
                    stringField = "",
                    nullableField = null,
                    floatField = null,
                    nestedField = NestedEntity(
                        booleanField = false,
                        nestedField = NestedEntity(
                            booleanField = true,
                            nestedField = NestedEntity(true)
                        )
                    )
                )
            ),
            "all update" to Case(
                json = "{\n  \"int\": 0,\n  \"string\": \"\",\n  \"float\": 0.0,\n  \"nullable\": null,\n  \"nested\": {\n    \"bf\": true\n  }\n}",
                expected = Entity(
                    intField = 0,
                    stringField = "",
                    floatField = 0.0,
                    nullableField = null,
                    nestedField = NestedEntity(true)
                )
            )
        ).testCases {
            val patch: EntityPatch = jackson.readValue(json)
            log.info("Patch: {}", patch)

            log.info("Expected: {}", expected)

            var entity = newEntity()
            patch.update(entity)
            log.info("Updated: {}", entity)
            assertEquals(expected, entity)

            entity = newEntity()
            val updated = patch.updated(entity)
            log.info("Updated: {}", updated)
            assertEquals(expected, updated)
            assertEquals(newEntity(), entity)
        }
    }

    @Test
    fun `Test basic patch cases`() {
        data class Case(
            val json: String,
            val expected: Entity
        )

        var entity = newEntity()

        mapOf(
            "int patch" to Case(
                json = """{ "int": 0 }""",
                expected = entity.copy(intField = 0)
            ),
            "string patch" to Case(
                json = """{ "string": "elite" }""",
                expected = entity.copy(stringField = "elite")
            ),
            "float patch" to Case(
                json = """{ "float": 11.12 }""",
                expected = entity.copy(floatField = 11.12)
            ),
            "null patch" to Case(
                json = """{ "nullable": null }""",
                expected = entity.copy(nullableField = null)
            ),
            "nested patch" to Case(
                json = "{ \n  \"nested\": {\n    \"bf\": true\n  }\n}",
                expected = entity.copy(nestedField = NestedEntity(true))
            ),
            "nested instance creation" to Case(
                json = "{ \n  \"nested\": {\n    \"nf\": {\n      \"bf\":true\n    }\n  }\n}",
                expected = entity.copy(
                    nestedField = NestedEntity(
                        booleanField = false,
                        nestedField = NestedEntity(true)
                    )
                )
            ),
            "double nested instance creation" to Case(
                json = "{ \n  \"nested\": {\n    \"nf\": {\n      \"bf\":true,\n      \"nf\": {\n        \"bf\":true\n      }\n    }\n  }\n}",
                expected = entity.copy(
                    nestedField = NestedEntity(
                        booleanField = false,
                        nestedField = NestedEntity(
                            booleanField = true,
                            nestedField = NestedEntity(true)
                        )
                    )
                )
            ),
            "all patch" to Case(
                json = "{\n  \"int\": 0,\n  \"string\": \"\",\n  \"float\": 0.0,\n  \"nullable\": null,\n  \"nested\": {\n    \"bf\": true\n  }\n}",
                expected = Entity(
                    intField = 0,
                    stringField = "",
                    floatField = 0.0,
                    nullableField = null,
                    nestedField = NestedEntity(true)
                )
            )
        ).testCases {
            val patch: EntityPatch = jackson.readValue(json)
            log.info("Patch: {}", patch)

            log.info("Expected: {}", expected)

            entity = newEntity()
            patch.patch(entity)
            log.info("Patched: {}", entity)
            assertEquals(expected, entity)

            entity = newEntity()
            val patched = patch.patched(entity)
            log.info("Patched: {}", patched)
            assertEquals(expected, patched)
            assertEquals(newEntity(), entity)
        }
    }

    @Test
    fun `Test directly accessing patch values`() {
        val json = "{\n  \"int\": 0,\n  \"string\": \"\",\n  \"float\": 0.0,\n  \"nullable\": null,\n  \"nested\": {\n    \"bf\": true\n  }\n}"
        val patch: EntityPatch = jackson.readValue(json)

        assertEquals(0, patch.int)
        assertEquals("", patch.string)
        assertEquals(0.0, patch.float)
        assertEquals(null, patch.nullable)
        assertEquals(true, patch.nested.bf)
    }

    @Test
    fun `Should throw when directly accessing unset values`() {
        val json = "{}"
        val patch: EntityPatch = jackson.readValue(json)

        assertThrows<InputException> {
            patch.int
        }
        assertThrows<InputException> {
            patch.nullable
        }
        assertThrows<InputException> {
            patch.nested.bf
        }
    }

    @Test
    fun `Should throw when delegating to read-only property`() {
        class MyPatch : PatchOf<Entity>() {
            val intField by patchOf(Entity::intField)
        }

        assertThrows<UnsupportedOperationException> {
            MyPatch()
        }
    }

    @Test
    fun `Should throw when delegating to more than one class`() {
        open class MyEntity(val field: String)
        class MySubEntity(val field2: String) : MyEntity(field2)

        class MyPatch : PatchOf<MyEntity>() {
            var field by patchOf(MyEntity::field)
            var field2 by patchOf(MySubEntity::field2)
        }

        assertThrows<IllegalArgumentException>() {
            MyPatch()
        }
    }

    @Test
    fun `Should throw when updating or patching in place a read-only property`() {
        data class MyEntity(
            val prop: String
        )

        class MyPatch : PatchOf<MyEntity>() {
            var prop by patchOf(MyEntity::prop)
        }

        val patch = MyPatch().apply {
            prop = "PATCH"
        }

        assertThrows<UnsupportedOperationException> {
            patch.update(MyEntity("VALUE"))
        }

        assertThrows<UnsupportedOperationException> {
            patch.patch(MyEntity("VALUE"))
        }
    }

    @Test
    fun `Should throw when accessing not set property`() {
        val patch = EntityPatch()

        assertThrows<InputException> {
            patch.int
        }
    }

    @Test
    fun `Should throw when updating or patching null object`() {
        val patch = object : PatchOf<Entity?>() {
            var field by patchOf(Entity::floatField)
        }

        assertThrows<IllegalArgumentException> {
            patch.update(null)
        }
        assertThrows<IllegalArgumentException> {
            patch.updated(null)
        }
        assertThrows<IllegalArgumentException> {
            patch.patch(null)
        }
        assertThrows<IllegalArgumentException> {
            patch.patched(null)
        }
    }

    @Test
    fun `Should throw when field missing for nested instantiation`() {
        val json = "{\n  \"int\": 0,\n  \"string\": \"\",\n  \"nested\": {}\n}"
        val patch: EntityPatch = jackson.readValue(json)

        val entity = newEntity().copy(nestedField = null)

        assertThrows<InputException> {
            patch.patch(entity)
        }
        assertThrows<InputException> {
            patch.patched(entity)
        }
        assertThrows<InputException> {
            patch.update(entity)
        }
        assertThrows<InputException> {
            patch.updated(entity)
        }
    }

    @Test
    fun `Should throw when field missing for update`() {
        val json = "{\n  \"int\": 0\n}"
        val patch: EntityPatch = jackson.readValue(json)

        assertThrows<InputException> {
            val entity = newEntity()
            patch.update(entity)
        }
        assertThrows<InputException> {
            val entity = newEntity()
            patch.updated(entity)
        }
    }

    @Test
    fun `Should throw when updating or patching non-data class`() {
        class NonData(var field: String? = null)

        val patch = object : PatchOf<NonData>() {
            var field by patchOf(NonData::field)
        }
        val entity = NonData()

        patch.update(entity)
        patch.patch(entity)

        assertThrows<UnsupportedOperationException> {
            patch.updated(entity)
        }

        assertThrows<UnsupportedOperationException> {
            patch.patched(entity)
        }
    }

    @Test
    fun `Should throw when there are no delegates`() {
        data class Data(val field: String)

        val patch = object : PatchOf<Data>() {}
        val entity = Data("value")

        assertThrows<UnsupportedOperationException> {
            patch.updated(entity)
        }

        assertThrows<UnsupportedOperationException> {
            patch.patched(entity)
        }
    }

    @Test
    fun `Should throw when updating or patching property outside of primary constructor`() {
        class NonPrimary {
            var field: String = "value"
        }

        val patch = object : PatchOf<NonPrimary>() {
            var field by patchOf(NonPrimary::field)
        }

        patch.field = "NewValue"

        patch.update(NonPrimary())
        patch.patch(NonPrimary())

        assertThrows<UnsupportedOperationException> {
            patch.updated(NonPrimary())
        }
        assertThrows<UnsupportedOperationException> {
            patch.patched(NonPrimary())
        }
    }

    @Test
    fun `Should throw when nested instantiated object patches property outside of primary constructor`() {
        class MyNestedEntity {
            var field: String = "value"
        }

        data class MyEntity(
            var field: String = "value",
            var nested: MyNestedEntity? = null
        )

        class NestedPatch : PatchOf<MyNestedEntity?>() {
            var field by patchOf(MyNestedEntity::field)
        }

        class Patch : PatchOf<MyEntity>() {
            var nested by patchOf(MyEntity::nested, by = NestedPatch::class)
        }

        val patch = Patch().apply {
            nested = NestedPatch()
        }

        assertThrows<UnsupportedOperationException> {
            patch.update(MyEntity())
        }
        assertThrows<UnsupportedOperationException> {
            patch.patch(MyEntity())
        }
        assertThrows<UnsupportedOperationException> {
            patch.updated(MyEntity())
        }
        assertThrows<UnsupportedOperationException> {
            patch.patched(MyEntity())
        }
    }

    @Test
    fun `Should throw when missing required fields`() {
        data class MyEntity(
            var field: String? = null // optional
        )

        val requiredPatch = object : PatchOf<MyEntity>() {
            var field by patchOf(MyEntity::field, required = true)
        }
        val optionalPatch = object : PatchOf<MyEntity>() {
            var field by patchOf(MyEntity::field, required = false)
        }

        assertDoesNotThrow {
            optionalPatch.instance()
            optionalPatch.patch(MyEntity())
            optionalPatch.patched(MyEntity())
            optionalPatch.update(MyEntity())
            optionalPatch.updated(MyEntity())
        }

        assertThrows<InputException> { requiredPatch.instance() }
        assertThrows<InputException> { requiredPatch.patch(MyEntity()) }
        assertThrows<InputException> { requiredPatch.patched(MyEntity()) }
        assertThrows<InputException> { requiredPatch.update(MyEntity()) }
        assertThrows<InputException> { requiredPatch.updated(MyEntity()) }
    }

    @Test
    fun `Should work with custom logic`() {
        val json = "{\n  \"intField\": 1,\n  \"specialField\": true\n}"
        val patch : CustomPatch = jackson.readValue(json)
        val obj = newEntity()

        patch.patch(obj)

        assertEquals(newEntity().copy(intField = 1, nullableField = null), obj)
    }

    @Test
    fun `Should work in full server configuration`() {
        val server = testServer {
            class UpdateRequest : Input<EntityPatch>() {
                override suspend fun Ctx.respond() {
                    val patch: EntityPatch = body()
                    when (call.request.httpMethod) {
                        Put -> {
                            patch.update(newEntity())
                            patch.updated(newEntity())
                        }
                        Patch -> {
                            patch.patch(newEntity())
                            patch.patched(newEntity())
                        }
                    }
                    call.respond(NoContent, EmptyContent)
                }
            }

            PUT("/put") { UpdateRequest() }
            PATCH("/patch") { UpdateRequest() }
        }

        server.start()

        with(server.handleRequest {
            uri = "/put"
            method = Put
            addHeader(ContentType, Application.Json.toString())
            setBody("{\n  \"int\": 0,\n  \"string\": \"\",\n  \"float\": 0.0,\n  \"nullable\": null,\n  \"nested\": {\n    \"bf\": true\n  }\n}")
        }) {
            assertEquals(NoContent, response.status())
            assertNull(response.content)
        }

        with(server.handleRequest {
            uri = "/patch"
            method = Patch
            addHeader(ContentType, Application.Json.toString())
            setBody("{\n  \"int\": 0,\n  \"string\": \"\",\n  \"float\": 0.0,\n  \"nullable\": null,\n  \"nested\": {\n    \"bf\": true\n  }\n}")
        }) {
            assertEquals(NoContent, response.status())
            assertNull(response.content)
        }

        server.stop(0, 0)
    }

    private fun newEntity() = Entity(
        intField = 1337,
        stringField = "required",
        floatField = 1.337,
        nullableField = "nullable",
        nestedField = NestedEntity(false)
    ).also {
        log.info("New entity: {}", it)
    }
}
