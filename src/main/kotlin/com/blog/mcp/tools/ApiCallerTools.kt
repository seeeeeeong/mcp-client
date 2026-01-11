package com.blog.mcp.tools

import com.blog.mcp.config.BlogApiProperties
import com.blog.mcp.swagger.SwaggerMcpProperties
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder

@Component
class ApiCallerTools(
    private val restTemplate: RestTemplate,
    private val objectMapper: ObjectMapper,
    private val blogApiProperties: BlogApiProperties,
    private val swaggerMcpProperties: SwaggerMcpProperties
) {

    private val allowedMethods = setOf("GET", "POST", "PUT", "PATCH", "DELETE")

    @Tool(description = "Call a blog-api endpoint with optional headers, query params, and JSON body")
    fun callApi(
        @ToolParam(description = "Service name") serviceName: String,
        @ToolParam(description = "HTTP method, e.g. GET") method: String,
        @ToolParam(description = "Request path, e.g. /posts/1") path: String,
        @ToolParam(description = "Headers map", required = false) headers: Map<String, String>?,
        @ToolParam(description = "Query params map", required = false) queryParams: Map<String, String>?,
        @ToolParam(description = "JSON body object or string", required = false) body: Any?
    ): String {
        val serviceError = validateServiceName(serviceName)
        if (serviceError != null) {
            return serviceError
        }

        if (path.startsWith("http://") || path.startsWith("https://")) {
            return errorResponse("Absolute URLs are not allowed", mapOf("path" to path))
        }

        val normalizedPath = if (path.startsWith("/")) path else "/$path"
        val baseUrl = blogApiProperties.baseUrl.trimEnd('/')
        val methodValue = method.uppercase()
        if (!allowedMethods.contains(methodValue)) {
            return errorResponse("Method not allowed", mapOf("method" to methodValue))
        }

        val httpMethod = try {
            HttpMethod.valueOf(methodValue)
        } catch (ex: IllegalArgumentException) {
            return errorResponse("Invalid method", mapOf("method" to methodValue))
        }

        val uriBuilder = UriComponentsBuilder.fromHttpUrl(baseUrl + normalizedPath)
        queryParams?.forEach { (key, value) ->
            uriBuilder.queryParam(key, value)
        }

        val httpHeaders = HttpHeaders()
        headers?.forEach { (key, value) ->
            httpHeaders.add(key, value)
        }
        if (body != null && httpHeaders.contentType == null) {
            httpHeaders.contentType = MediaType.APPLICATION_JSON
        }

        val requestEntity = HttpEntity(body, httpHeaders)
        return try {
            val response = restTemplate.exchange(
                uriBuilder.build(true).toUri(),
                httpMethod,
                requestEntity,
                String::class.java
            )
            objectMapper.writeValueAsString(
                mapOf(
                    "status" to response.statusCode.value(),
                    "headers" to response.headers.toSingleValueMap(),
                    "body" to response.body
                )
            )
        } catch (ex: HttpStatusCodeException) {
            objectMapper.writeValueAsString(
                mapOf(
                    "error" to "HTTP_ERROR",
                    "status" to ex.statusCode.value(),
                    "headers" to ex.responseHeaders?.toSingleValueMap(),
                    "body" to ex.responseBodyAsString
                )
            )
        } catch (ex: Exception) {
            errorResponse("REQUEST_FAILED", mapOf("message" to ex.message))
        }
    }

    private fun validateServiceName(serviceName: String): String? {
        if (serviceName != swaggerMcpProperties.serviceName) {
            return errorResponse(
                "Unknown service name",
                mapOf("serviceName" to serviceName, "expected" to swaggerMcpProperties.serviceName)
            )
        }
        return null
    }

    private fun errorResponse(message: String, data: Map<String, Any?> = emptyMap()): String {
        val payload = linkedMapOf<String, Any?>("error" to message)
        payload.putAll(data)
        return objectMapper.writeValueAsString(payload)
    }
}
