package com.mcp.swagger

import com.fasterxml.jackson.databind.JsonNode

data class OpenApiSnapshot(
    val document: JsonNode,
    val index: OpenApiIndex
)

data class OpenApiIndex(
    val apiGroups: List<String>,
    val apiIndex: Map<String, Map<String, ApiMethodEntry>>
)

data class ApiMethodEntry(
    val tags: List<String>,
    val operationId: String?,
    val summary: String?
)

object OpenApiIndexBuilder {
    fun build(openApi: JsonNode): OpenApiIndex {
        val groups = linkedSetOf<String>()
        openApi.path("tags").forEach { tag ->
            tag.path("name").takeIf { it.isTextual }?.asText()?.let { groups.add(it) }
        }

        val pathsNode = openApi.path("paths")
        val paths = linkedMapOf<String, Map<String, ApiMethodEntry>>()
        pathsNode.fields().forEach { (path, pathItem) ->
            val methods = linkedMapOf<String, ApiMethodEntry>()
            pathItem.fields().forEach { (method, operation) ->
                val tags = operation.path("tags").map { it.asText() }
                tags.forEach { groups.add(it) }
                methods[method.lowercase()] = ApiMethodEntry(
                    tags = tags,
                    operationId = textOrNull(operation, "operationId"),
                    summary = textOrNull(operation, "summary")
                )
            }
            if (methods.isNotEmpty()) {
                paths[path] = methods
            }
        }

        return OpenApiIndex(
            apiGroups = groups.toList().sorted(),
            apiIndex = paths
        )
    }

    private fun textOrNull(node: JsonNode, field: String): String? {
        val value = node.path(field)
        return if (value.isTextual) value.asText() else null
    }
}
