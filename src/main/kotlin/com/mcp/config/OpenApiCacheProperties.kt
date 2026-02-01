package com.mcp.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "mcp.cache")
data class OpenApiCacheProperties(
    val ttlSeconds: Long = 60
)
