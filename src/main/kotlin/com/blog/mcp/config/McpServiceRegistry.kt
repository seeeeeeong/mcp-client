package com.blog.mcp.config

import org.springframework.stereotype.Component

@Component
class McpServiceRegistry(
    private val properties: McpServicesProperties
) {
    private val serviceMap: Map<String, McpServiceProperties> = properties.services
        .filter { it.name.isNotBlank() }
        .associateBy { it.name }

    fun list(): List<McpServiceProperties> = properties.services

    fun get(serviceName: String): McpServiceProperties? = serviceMap[serviceName]
}
