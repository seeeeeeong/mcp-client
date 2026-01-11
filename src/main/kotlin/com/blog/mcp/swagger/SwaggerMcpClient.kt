package com.blog.mcp.swagger

import com.blog.mcp.config.BlogApiProperties
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class SwaggerMcpClient(
    private val restTemplate: RestTemplate,
    private val objectMapper: ObjectMapper,
    private val blogApiProperties: BlogApiProperties,
    private val swaggerMcpProperties: SwaggerMcpProperties
) {

    fun fetchOpenApiJson(): JsonNode {
        val baseUrl = blogApiProperties.baseUrl.trimEnd('/')
        val docsPath = swaggerMcpProperties.apiDocsPath
        val docsUrl = baseUrl + docsPath
        val response = restTemplate.getForObject(docsUrl, String::class.java).orEmpty()
        return objectMapper.readTree(response)
    }
}
