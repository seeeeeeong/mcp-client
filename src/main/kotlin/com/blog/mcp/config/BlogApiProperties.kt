package com.blog.mcp.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "blog.api")
data class BlogApiProperties(
    val baseUrl: String = "http://localhost:8080"
)
