package com.mcp.support

import com.mcp.config.McpServiceProperties
import com.mcp.config.McpServiceRegistry
import com.fasterxml.jackson.databind.ObjectMapper

open class McpToolSupport(
    protected val objectMapper: ObjectMapper,
    protected val registry: McpServiceRegistry
) {

    protected fun resolveService(serviceName: String): McpServiceProperties? = registry.get(serviceName)

    protected fun withService(
        serviceName: String,
        block: (McpServiceProperties) -> String
    ): String {
        val service = resolveService(serviceName) ?: return serviceNotFound(serviceName)
        if (service.baseUrl.isBlank()) {
            return errorResponse("Base URL is empty", mapOf("serviceName" to serviceName))
        }
        return block(service)
    }

    protected fun serviceNotFound(serviceName: String): String =
        errorResponse(
            "Unknown service name",
            mapOf("serviceName" to serviceName, "available" to availableServices())
        )

    protected fun availableServices(): List<String> = registry.list()
        .map { it.name }
        .filter { it.isNotBlank() }
        .sorted()

    protected fun errorResponse(message: String, data: Map<String, Any?> = emptyMap()): String {
        val payload = linkedMapOf<String, Any?>("error" to message)
        payload.putAll(data)
        return objectMapper.writeValueAsString(payload)
    }
}
