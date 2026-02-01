package com.mcp.swagger

import com.mcp.config.McpServiceProperties
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class SwaggerMcpClient(
    private val restTemplate: RestTemplate,
    private val objectMapper: ObjectMapper
) {

    fun fetchOpenApiJson(service: McpServiceProperties): JsonNode {
        val baseUrl = service.baseUrl.trimEnd('/')
        val docsPath = normalizePath(service.apiDocsPath)
        val docsUrl = baseUrl + docsPath
        val response = restTemplate.getForObject(docsUrl, String::class.java).orEmpty()
        return objectMapper.readTree(response)
    }

    private fun normalizePath(path: String): String =
        if (path.startsWith("/")) path else "/$path"
}
