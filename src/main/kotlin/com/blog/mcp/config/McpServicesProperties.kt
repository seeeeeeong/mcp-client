package com.blog.mcp.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "mcp")
data class McpServicesProperties(
    val services: List<McpServiceProperties> = emptyList()
)

data class McpServiceProperties(
    val name: String = "",
    val baseUrl: String = "",
    val apiDocsPath: String = "/v3/api-docs"
)
