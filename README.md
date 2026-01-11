# mcp-client

Local MCP server for testing `blog-api` with an Inspector/CLI client. This server exposes Swagger discovery tools and a tool that can call blog-api endpoints.

## Prereqs
- blog-api running at `http://localhost:8080`
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
  - `serviceName`: `blog-api`
  - `method`: `GET|POST|PUT|PATCH|DELETE`
  - `path`: `/posts/1`
  - `headers`: map (optional)
  - `queryParams`: map (optional)
  - `body`: JSON object or string (optional)

## Configuration
`src/main/resources/application.yml`
```yaml
server:
  port: 8090

blog:
  api:
    base-url: http://localhost:8080

swagger:
  mcp:
    service-name: blog-api
    api-docs-path: /v3/api-docs
```

## Example calls
Example `listApis` call:
```json
{
  "serviceName": "blog-api",
  "apiGroup": "Posts"
}
```

Example `callApi` call:
```json
{
  "serviceName": "blog-api",
  "method": "GET",
  "path": "/posts/1"
}
```
