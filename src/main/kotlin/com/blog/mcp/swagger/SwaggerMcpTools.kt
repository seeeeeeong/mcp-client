package com.blog.mcp.swagger

import com.blog.mcp.config.McpServiceRegistry
import com.blog.mcp.support.McpToolSupport
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam
import org.springframework.stereotype.Component

@Component
class SwaggerMcpTools(
    private val openApiCache: OpenApiCache,
    objectMapper: ObjectMapper,
    registry: McpServiceRegistry
) : McpToolSupport(objectMapper, registry) {

    @Tool(description = "List services and API groups available in this Swagger MCP server")
    fun listServices(): String {
        val services = registry.list().filter { it.name.isNotBlank() }
        val result = services.map { service ->
            if (service.baseUrl.isBlank()) {
                mapOf(
                    "serviceName" to service.name,
                    "error" to "MISSING_BASE_URL"
                )
            } else {
                try {
                    val snapshot = openApiCache.get(service)
                    mapOf(
                        "serviceName" to service.name,
                        "apiGroups" to snapshot.index.apiGroups
                    )
                } catch (ex: Exception) {
                    mapOf(
                        "serviceName" to service.name,
                        "error" to "OPENAPI_FETCH_FAILED",
                        "message" to (ex.message ?: "unknown")
                    )
                }
            }
        }
        return objectMapper.writeValueAsString(result)
    }

    @Tool(description = "List APIs for a service, optionally filtered by API group (tag)")
    fun listApis(
        @ToolParam(description = "Service name to search") serviceName: String,
        @ToolParam(description = "API group tag to filter", required = false) apiGroup: String?
    ): String = withService(serviceName) { service ->
        val index = openApiCache.get(service).index
        val result = linkedMapOf<String, Any>()

        index.apiIndex.forEach { (path, methods) ->
            val filteredMethods = linkedMapOf<String, Any>()
            methods.forEach { (method, entry) ->
                val tags = entry.tags
                if (!apiGroup.isNullOrBlank() && tags.none { it.equals(apiGroup, ignoreCase = true) }) {
                    return@forEach
                }
                val entryMap = linkedMapOf<String, Any?>(
                    "tags" to tags,
                    "operationId" to entry.operationId,
                    "summary" to entry.summary
                ).filterValues { it != null }
                if (entryMap.isNotEmpty()) {
                    filteredMethods[method] = entryMap
                }
            }
            if (filteredMethods.isNotEmpty()) {
                result[path] = filteredMethods
            }
        }

        objectMapper.writeValueAsString(result)
    }

    @Tool(description = "Get detailed API info for a path and method")
    fun getApiDetail(
        @ToolParam(description = "Service name") serviceName: String,
        @ToolParam(description = "Request path, e.g. /posts/{id}") requestUrl: String,
        @ToolParam(description = "HTTP method, e.g. GET") httpMethod: String
    ): String = withService(serviceName) { service ->
        val openApi = openApiCache.get(service).document
        val pathNode = openApi.path("paths").path(requestUrl)
        if (pathNode.isMissingNode) {
            return@withService objectMapper.writeValueAsString(mapOf("error" to "Path not found", "path" to requestUrl))
        }

        val methodKey = httpMethod.lowercase()
        val methodNode = pathNode.path(methodKey)
        if (methodNode.isMissingNode) {
            return@withService objectMapper.writeValueAsString(
                mapOf(
                    "error" to "Method not found for path",
                    "path" to requestUrl,
                    "method" to methodKey
                )
            )
        }

        val parameters = methodNode.path("parameters")
            .filter { it.isObject }
            .map { param ->
                val schemaNode = resolveSchemaNode(param)
                linkedMapOf<String, Any?>(
                    "name" to textOrNull(param, "name"),
                    "in" to textOrNull(param, "in"),
                    "required" to param.path("required").takeIf { it.isBoolean }?.asBoolean(),
                    "schema" to schemaNode?.let { extractSchema(it) }
                ).filterValues { it != null }
            }

        val requestBodyNode = methodNode.path("requestBody")
        val requestSchemaNode = resolveSchemaNodeFromContent(requestBodyNode.path("content"))
        val requestBody = if (requestSchemaNode != null && !requestSchemaNode.isMissingNode) {
            linkedMapOf<String, Any?>(
                "required" to requestBodyNode.path("required").takeIf { it.isBoolean }?.asBoolean(),
                "schema" to extractSchema(requestSchemaNode)
            ).filterValues { it != null }
        } else {
            null
        }

        val responsesNode = methodNode.path("responses")
        val responses = linkedMapOf<String, Any>()
        responsesNode.fields().forEach { (status, response) ->
            val schemaNode = resolveSchemaNodeFromContent(response.path("content"))
            if (schemaNode != null && !schemaNode.isMissingNode) {
                responses[status] = extractSchema(schemaNode)
            }
        }

        val componentRefs = linkedSetOf<String>()
        collectRefs(requestSchemaNode, componentRefs)
        responses.values.forEach { schema ->
            collectRefsFromMap(schema, componentRefs)
        }

        val result = linkedMapOf<String, Any?>(
            "parameters" to parameters,
            "requestBody" to requestBody,
            "responses" to responses,
            "componentRefs" to componentRefs.toList()
        ).filterValues { it != null }

        objectMapper.writeValueAsString(result)
    }

    @Tool(description = "Get component schemas by ref")
    fun getComponentSchemas(
        @ToolParam(description = "Service name") serviceName: String,
        @ToolParam(
            description = "Comma-separated component refs, e.g. #/components/schemas/ErrorMessage",
            required = true
        ) refs: String
    ): String = withService(serviceName) { service ->
        val openApi = openApiCache.get(service).document
        val schemasNode = openApi.path("components").path("schemas")
        val refList = refs.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val result = linkedMapOf<String, Any>()

        refList.forEach { ref ->
            val name = ref.substringAfterLast("/")
            val schemaNode = schemasNode.path(name)
            if (!schemaNode.isMissingNode) {
                result[ref] = extractSchema(schemaNode)
            }
        }

        objectMapper.writeValueAsString(result)
    }

    private fun textOrNull(node: JsonNode, field: String): String? {
        val value = node.path(field)
        return if (value.isTextual) value.asText() else null
    }

    private fun resolveSchemaNode(parameterNode: JsonNode): JsonNode? {
        val schemaNode = parameterNode.path("schema")
        if (!schemaNode.isMissingNode) {
            return schemaNode
        }
        return resolveSchemaNodeFromContent(parameterNode.path("content"))
    }

    private fun resolveSchemaNodeFromContent(contentNode: JsonNode): JsonNode? {
        if (contentNode.isMissingNode) {
            return null
        }
        val jsonNode = contentNode.path("application/json")
        if (!jsonNode.isMissingNode) {
            return jsonNode.path("schema")
        }
        val first = contentNode.fields().asSequence().firstOrNull()?.value
        return first?.path("schema")
    }

    private fun extractSchema(schemaNode: JsonNode): Map<String, Any?> {
        if (schemaNode.has("\$ref")) {
            return mapOf("\$ref" to schemaNode.path("\$ref").asText())
        }

        val result = linkedMapOf<String, Any?>()
        schemaNode.path("type").takeIf { it.isTextual }?.asText()?.let { result["type"] = it }
        schemaNode.path("format").takeIf { it.isTextual }?.asText()?.let { result["format"] = it }
        schemaNode.path("enum").takeIf { it.isArray }?.let { enumNode ->
            result["enum"] = enumNode.map { it.asText() }
        }
        schemaNode.path("required").takeIf { it.isArray }?.let { requiredNode ->
            result["required"] = requiredNode.map { it.asText() }
        }
        schemaNode.path("properties").takeIf { it.isObject }?.let { props ->
            val properties = linkedMapOf<String, Any?>()
            props.fields().forEach { (name, property) ->
                properties[name] = extractSchema(property)
            }
            result["properties"] = properties
        }
        schemaNode.path("items").takeIf { !it.isMissingNode }?.let { items ->
            result["items"] = extractSchema(items)
        }
        return result
    }

    private fun collectRefs(schemaNode: JsonNode?, refs: MutableSet<String>) {
        if (schemaNode == null || schemaNode.isMissingNode) {
            return
        }
        if (schemaNode.has("\$ref")) {
            refs.add(schemaNode.path("\$ref").asText())
            return
        }
        schemaNode.path("properties").takeIf { it.isObject }?.fields()?.forEach { (_, property) ->
            collectRefs(property, refs)
        }
        schemaNode.path("items").takeIf { !it.isMissingNode }?.let { items ->
            collectRefs(items, refs)
        }
        schemaNode.path("allOf").takeIf { it.isArray }?.forEach { node ->
            collectRefs(node, refs)
        }
        schemaNode.path("oneOf").takeIf { it.isArray }?.forEach { node ->
            collectRefs(node, refs)
        }
        schemaNode.path("anyOf").takeIf { it.isArray }?.forEach { node ->
            collectRefs(node, refs)
        }
    }

    private fun collectRefsFromMap(schema: Any?, refs: MutableSet<String>) {
        when (schema) {
            is Map<*, *> -> {
                val ref = schema["\$ref"]
                if (ref is String) {
                    refs.add(ref)
                }
                schema.values.forEach { collectRefsFromMap(it, refs) }
            }
            is List<*> -> schema.forEach { collectRefsFromMap(it, refs) }
        }
    }
}
