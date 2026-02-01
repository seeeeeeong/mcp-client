# mcp-client

Local MCP server that aggregates Swagger/OpenAPI from multiple services and exposes MCP tools for discovery and API calls.

## Prereqs
- Each target service running locally (e.g. blog-api at http://localhost:8080)
- Java 17

## Run
```bash
./gradlew bootRun
```

The MCP server runs on port `8090` by default.

## MCP Endpoint
Spring AI MCP server uses a Streamable HTTP endpoint (default is typically `/mcp`).
If your client cannot connect, check the startup logs for the exact MCP endpoint path.

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
```

## Example calls
Example `listApis` call:
```json
{
  "serviceName": "blog-api",
  "apiGroup": "Post"
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
