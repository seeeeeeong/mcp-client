package com.mcp.config

import com.mcp.swagger.SwaggerMcpTools
import com.mcp.tools.ApiCallerTools
import org.springframework.ai.tool.ToolCallbackProvider
import org.springframework.ai.tool.method.MethodToolCallbackProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class McpConfig {

    @Bean
    fun mcpToolCallbacks(
        swaggerMcpTools: SwaggerMcpTools,
        apiCallerTools: ApiCallerTools
    ): ToolCallbackProvider {
        return MethodToolCallbackProvider.builder()
            .toolObjects(swaggerMcpTools, apiCallerTools)
            .build()
    }
}
