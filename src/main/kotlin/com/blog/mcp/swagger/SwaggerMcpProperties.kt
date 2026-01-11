package com.blog.mcp.swagger

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "swagger.mcp")
data class SwaggerMcpProperties(
    val serviceName: String = "blog-api",
    val apiDocsPath: String = "/v3/api-docs"
)
