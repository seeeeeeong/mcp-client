package com.blog.mcp.config

import com.blog.mcp.swagger.SwaggerMcpProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate

@Configuration
@EnableConfigurationProperties(
    BlogApiProperties::class,
    SwaggerMcpProperties::class
)
class AppConfig {

    @Bean
    fun restTemplate(): RestTemplate {
        return RestTemplate()
    }
}
