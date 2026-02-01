package com.mcp.swagger

import com.mcp.config.McpServiceProperties
import com.mcp.config.OpenApiCacheProperties
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

@Component
class OpenApiCache(
    private val client: SwaggerMcpClient,
    private val cacheProperties: OpenApiCacheProperties,
    private val meterRegistry: MeterRegistry
) {
    private val cache = ConcurrentHashMap<String, CachedOpenApi>()

    init {
        Gauge.builder("mcp.openapi.cache.size") { cache.size.toDouble() }
            .register(meterRegistry)
    }

    fun get(service: McpServiceProperties): OpenApiSnapshot {
        val ttlSeconds = cacheProperties.ttlSeconds
        if (ttlSeconds <= 0) {
            return fetch(service)
        }

        val now = Instant.now()
        val cached = cache[service.name]
        if (cached != null && now.isBefore(cached.expiresAt)) {
            meterRegistry.counter("mcp.openapi.cache.hit", "service", service.name).increment()
            return cached.snapshot
        }

        meterRegistry.counter("mcp.openapi.cache.miss", "service", service.name).increment()
        val snapshot = fetch(service)
        cache[service.name] = CachedOpenApi(snapshot, now.plusSeconds(ttlSeconds))
        return snapshot
    }

    private fun fetch(service: McpServiceProperties): OpenApiSnapshot {
        val timer = Timer.builder("mcp.openapi.fetch")
            .tag("service", service.name)
            .register(meterRegistry)
        val sample = Timer.start(meterRegistry)
        return try {
            val document = client.fetchOpenApiJson(service)
            val index = OpenApiIndexBuilder.build(document)
            sample.stop(timer)
            OpenApiSnapshot(document, index)
        } catch (ex: Exception) {
            sample.stop(timer)
            meterRegistry.counter("mcp.openapi.fetch.error", "service", service.name).increment()
            throw ex
        }
    }

    private data class CachedOpenApi(
        val snapshot: OpenApiSnapshot,
        val expiresAt: Instant
    )
}
