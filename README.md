# Swagger MCP Client

A local MCP server that aggregates Swagger/OpenAPI from multiple services and exposes MCP tools for discovery and API calls.

## What this is
- One MCP endpoint that lets an LLM (or any client) **discover** APIs and **call** them across multiple projects.
- Works with Spring-based services that expose OpenAPI (`/v3/api-docs`).

## Key features
- **Multi-service registry**: register multiple projects and switch via `serviceName`.
- **Swagger discovery**: list APIs, inspect schemas, and fetch component refs.
- **Direct API calls**: `callApi` for quick end-to-end verification.
- **Observability**: Micrometer + Actuator metrics for cache hit/miss and fetch timing.
- **Performance**: OpenAPI snapshot cache + index to reduce repeated parsing cost.

## Architecture 
1) MCP client calls `listApis`/`getApiDetail`/`getComponentSchemas`
2) Server fetches OpenAPI JSON (once per TTL), builds index, returns simplified JSON
3) `callApi` proxies requests to the target service using the registry


## Prereqs
- Java 17
- Target services running (e.g., `blog-api` at `http://localhost:8080`)

## Run
```bash
./gradlew bootRun
```

Default MCP server port: `8090`
Main application class: `com.mcp.McpClientApplication`

## MCP Endpoint
Spring AI MCP server uses a Streamable HTTP endpoint (default is `/mcp`).
If your client cannot connect, check the startup logs for the exact MCP endpoint path.

## Configuration
`src/main/resources/application.yml`
```yaml
server:
  port: 8090

mcp:
  services:
    - name: blog-api
      base-url: http://localhost:8080
      api-docs-path: /v3/api-docs

  cache:
    ttl-seconds: 60
```

## Tools
### Swagger discovery
- `listServices`
- `listApis`
- `getApiDetail`
- `getComponentSchemas`

### API call
- `callApi`
  - `serviceName`: service key in `mcp.services`
  - `method`: `GET|POST|PUT|PATCH|DELETE`
  - `path`: `/api/v1/posts/1`
  - `headers`: map (optional)
  - `queryParams`: map (optional)
  - `body`: JSON object or string (optional)

## Example calls
Example `listApis` call:
```json
{
  "serviceName": "blog-api",
  "apiGroup": "Post"
}
```

Example `getApiDetail` call:
```json
{
  "serviceName": "blog-api",
  "requestUrl": "/api/v1/posts/{postId}",
  "httpMethod": "GET"
}
```

Example `callApi` call:
```json
{
  "serviceName": "blog-api",
  "method": "GET",
  "path": "/api/v1/posts/1"
}
```

## Observability
Actuator metrics 
- `mcp.openapi.cache.hit`
- `mcp.openapi.cache.miss`
- `mcp.openapi.fetch`

Example:
```bash
curl http://localhost:8090/actuator/metrics/mcp.openapi.cache.hit
```
