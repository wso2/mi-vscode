/**
 * Copyright (c) 2026, WSO2 LLC. (https://www.wso2.com) All Rights Reserved.
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * MCP Server Reference (exposing MI integrations as Model Context Protocol tools).
 * Covers the MCP inbound endpoint, the <mcptools> local entry, input schema rules,
 * and the exact file/naming conventions the VS Code tooling expects.
 *
 * Requires MI runtime 4.6.0+ and the `mi-inbound-mcp` inbound connector.
 *
 * Section-based exports for granular context loading.
 */

export const MCP_SERVER_REFERENCE_SECTIONS: Record<string, string> = {

overview: `## MCP Server — Overview

An **MCP Server** exposes integrations deployed in WSO2 MI as [Model Context Protocol](https://modelcontextprotocol.io) tools that AI agents/assistants (Claude Desktop, GitHub Copilot, MI AI-connector agents, etc.) can discover and invoke.

**Requires MI runtime 4.6.0+** and the **MCP inbound connector** (\`mi-inbound-mcp\`, Maven group \`org.wso2.integration.inbound\`, type \`zip\`) as a project dependency.

An MCP server is exactly **two artifacts**, linked by a naming convention:

| Artifact | File | Purpose |
|----------|------|---------|
| Inbound endpoint | \`src/main/wso2mi/artifacts/inbound-endpoints/<ServerName>.xml\` | Listener on a dedicated port; \`class="org.wso2.carbon.inbound.sse.McpInboundListener"\` |
| Local entry | \`src/main/wso2mi/artifacts/local-entries/<ServerName>-mcp-config.xml\` | \`<mcptools>\` tool definitions; key MUST be \`<ServerName>-mcp-config\` |

The inbound endpoint references the local entry via its \`mcp.tools.localentry\` parameter.

**CRITICAL naming rule**: the local entry key/file MUST be \`<ServerName>-mcp-config\` — the VS Code project explorer discovers MCP servers by this \`-mcp-config.xml\` suffix and derives the server name from it. A different key breaks the MCP Servers tree view and the Edit MCP Server form.

Each tool is backed by either:
- a **REST API resource** deployed in the same project (tool args → path/query params + JSON body), or
- a **sequence** (tool args injected as JSON payload; the payload at the end of the sequence is the tool result).

Runtime behavior: serves MCP over the **streamable HTTP transport** at the fixed \`/mcp\` context path — server URL is \`http://<host>:<port>/mcp\`. Implements MCP protocol version \`2024-11-05\`; JSON-RPC methods \`initialize\`, \`notifications/initialized\`, \`tools/list\`, \`tools/call\`, \`ping\`. Sessions tracked via the \`Mcp-Session-Id\` HTTP header; \`GET\` with \`Accept: text/event-stream\` opens an SSE stream with periodic keep-alives.`,

inbound_endpoint: `## MCP Inbound Endpoint (\`<inboundEndpoint>\`)

A custom (class-based) inbound endpoint — there is no \`protocol\` attribute; the \`class\` attribute selects the MCP listener.

\`\`\`xml
<?xml version="1.0" encoding="UTF-8"?>
<inboundEndpoint name="WeatherMCPServer" sequence="" onError="" class="org.wso2.carbon.inbound.sse.McpInboundListener">
    <parameters xmlns="http://ws.apache.org/ns/synapse">
        <parameter name="inbound.mcp.port">8300</parameter>
        <parameter name="inbound.http.port">8300</parameter>
        <parameter name="inbound.http.context">/mcp</parameter>
        <parameter name="mcp.tools.localentry">WeatherMCPServer-mcp-config</parameter>
        <parameter name="inbound.behavior">listening</parameter>
        <parameter name="inbound.cors.allow.origin">*</parameter>
        <parameter name="inbound.cors.allow.methods">GET, POST, OPTIONS</parameter>
        <parameter name="inbound.cors.allow.headers">Content-Type, Mcp-Session-Id</parameter>
        <parameter name="inbound.cors.expose.headers">Mcp-Session-Id</parameter>
        <parameter name="inbound.sse.keepalive.interval">30000</parameter>
    </parameters>
</inboundEndpoint>
\`\`\`

Note \`sequence=""\` and \`onError=""\` — an MCP server dispatches to the APIs/sequences named in its tool config, not to an injecting sequence.

### Required parameters
| Parameter | Notes |
|-----------|-------|
| \`class\` (attribute) | Must be \`org.wso2.carbon.inbound.sse.McpInboundListener\` |
| \`inbound.mcp.port\` | Listener port. Server fails to start if missing/non-numeric. Port offset is NOT applied by default (enable via \`inbound.port.offset.enable=true\` under \`[synapse_properties]\` in deployment.toml). Must not collide with another inbound endpoint in the project. Tooling default: \`8300\` |
| \`mcp.tools.localentry\` | Key of the local entry holding \`<mcptools>\` (i.e. \`<ServerName>-mcp-config\`). Fallback parameter \`mcp.tools\` is read if absent |

### Optional parameters
| Parameter | Default | Notes |
|-----------|---------|-------|
| \`inbound.cors.allow.origin\` | \`*\` | \`Access-Control-Allow-Origin\` |
| \`inbound.cors.allow.methods\` | \`GET, POST, OPTIONS\` | \`Access-Control-Allow-Methods\` |
| \`inbound.cors.allow.headers\` | \`Content-Type, Mcp-Session-Id\` | \`Access-Control-Allow-Headers\` |
| \`inbound.cors.expose.headers\` | \`Mcp-Session-Id\` | \`Access-Control-Expose-Headers\` |
| \`inbound.sse.keepalive.interval\` | \`30000\` | ms between SSE keep-alive comments; invalid value → default |

Always also emit \`inbound.http.port\` (same value as \`inbound.mcp.port\`), \`inbound.http.context\` = \`/mcp\`, and \`inbound.behavior\` = \`listening\` — the VS Code tooling generates and expects them (port-collision checks read \`inbound.http.port\`). The served context path is fixed at \`/mcp\` regardless.

### Path layout
\`src/main/wso2mi/artifacts/inbound-endpoints/<ServerName>.xml\`, \`artifact.xml\` type \`synapse/inbound-endpoint\`.`,

tools_local_entry: `## Tools Local Entry (\`<mcptools>\`)

An inline-XML local entry whose key is \`<ServerName>-mcp-config\`. One \`<tool>\` element per exposed tool. An empty \`<mcptools></mcptools>\` body is valid (server with no tools yet).

\`\`\`xml
<?xml version="1.0" encoding="UTF-8"?>
<localEntry key="WeatherMCPServer-mcp-config" xmlns="http://ws.apache.org/ns/synapse">
    <mcptools>
        <!-- API-backed tool: api + resource + method -->
        <tool name="get_weather">
            <api>WeatherAPI</api>
            <resource>/weather/{city}</resource>
            <method>GET</method>
            <description>Get the current weather for a city</description>
            <inputSchema><![CDATA[{"type":"object","properties":{"city":{"type":"string"}},"additionalProperties":false,"required":["city"]}]]></inputSchema>
        </tool>
        <!-- Sequence-backed tool: sequence instead of api/resource/method -->
        <tool name="convert_currency">
            <sequence>CurrencyConversionSequence</sequence>
            <description>Convert an amount from one currency to another</description>
            <inputSchema><![CDATA[{"type":"object","properties":{"amount":{"type":"number"},"from":{"type":"string"},"to":{"type":"string"}},"additionalProperties":false,"required":["amount","from","to"]}]]></inputSchema>
        </tool>
    </mcptools>
</localEntry>
\`\`\`

### \`<tool>\` children
| Element | Notes |
|---------|-------|
| \`name\` (attribute) | Tool name advertised in \`tools/list\`. Short, action-oriented, snake_case (e.g. \`get_weather\`). Tooling default for API tools: \`<METHOD>_<path_cleaned>\` (non-alphanumerics → \`_\`) |
| \`<api>\` | Name of the backing REST API (API-backed tools; together with \`<resource>\` + \`<method>\`) |
| \`<resource>\` | URI template of the API resource (e.g. \`/weather/{city}\`). Path/query params populated from tool arguments |
| \`<method>\` | HTTP method of the resource (\`GET\`, \`POST\`, ...) |
| \`<sequence>\` | Name of the backing sequence (sequence-backed tools; mutually exclusive with \`api\`/\`resource\`/\`method\`) |
| \`<description>\` | What the tool does and when to use it — AI agents choose tools based on this; write it carefully |
| \`<inputSchema>\` | JSON Schema for the tool arguments, wrapped in CDATA. Optional but strongly recommended |

### Execution semantics
- **API-backed**: arguments matching \`{placeholders}\` in \`<resource>\` become path params; schema properties beyond path params map to query params / JSON request body per the API definition. The resource's response payload is the tool result.
- **Sequence-backed**: the full arguments object is injected into the sequence as the JSON payload (\`\${payload.<arg>}\`). The message payload when the sequence ends is returned as the tool result.

### Path layout
\`src/main/wso2mi/artifacts/local-entries/<ServerName>-mcp-config.xml\`, \`artifact.xml\` type \`synapse/local-entry\`.`,

input_schema: `## Tool Input Schemas

\`<inputSchema>\` holds a standard [JSON Schema](https://json-schema.org/) object, CDATA-wrapped. Conventions used by the tooling-generated schemas — follow them when hand-writing:

- Root is always \`{"type":"object", "properties":{...}}\`.
- Set \`"additionalProperties": false\` and list mandatory args in \`"required"\`.
- Property types: \`string\`, \`number\`, \`integer\`, \`boolean\`, \`array\`, \`object\`. Add per-property \`"description"\` — agents read them.
- No args → \`{"type":"object","properties":{},"additionalProperties":false}\`.

### API-backed tools — deriving the schema
The VS Code tooling generates these from the API's OpenAPI definition (\`src/main/wso2mi/resources/api-definitions/<api>.yaml\`): path + query \`parameters\` become properties (path params always required), and the \`application/json\` \`requestBody\` schema is merged in (\`$ref\`/\`allOf\` resolved). When writing one manually, mirror that: one property per \`{placeholder}\` in the resource template, plus query params and body fields the resource actually reads.

### Sequence-backed tools
The schema is free-form — define exactly the arguments the sequence reads from \`\${payload}\`. Example: sequence reads \`\${payload.city}\` and \`\${payload.units}\`:

\`\`\`json
{"type":"object","properties":{"city":{"type":"string","description":"City name"},"units":{"type":"string","enum":["metric","imperial"],"description":"Unit system"}},"additionalProperties":false,"required":["city"]}
\`\`\``,

creation_recipe: `## Creating an MCP Server — Recipe

Given server name \`MyServer\` and port \`8300\`:

1. **Add the connector dependency**: add \`mi-inbound-mcp\` (inbound, group \`org.wso2.integration.inbound\`, type \`zip\`) to pom.xml via the connector-management tool. It provides \`org.wso2.carbon.inbound.sse.McpInboundListener\` at runtime — without it the CAR deploys but the inbound fails to start with a ClassNotFoundException.
2. **Pick a free port**: scan existing \`inbound-endpoints/*.xml\` for \`inbound.http.port\` / \`inbound.mcp.port\` values and avoid them (and 8290/8253/9201/9164).
3. **Create the local entry** \`local-entries/MyServer-mcp-config.xml\` with key \`MyServer-mcp-config\` and the \`<mcptools>\` tool definitions (empty \`<mcptools></mcptools>\` if tools come later).
4. **Create the inbound endpoint** \`inbound-endpoints/MyServer.xml\` with \`class="org.wso2.carbon.inbound.sse.McpInboundListener"\`, \`mcp.tools.localentry\` = \`MyServer-mcp-config\`, and the full parameter set (see inbound_endpoint section).
5. **Back each tool with a real artifact**: API tools need the named API + resource + method to exist in \`artifacts/apis/\`; sequence tools need the named sequence in \`artifacts/sequences/\`. Convention for from-scratch tools: create a sequence named \`<tool_name>_tool\`.
6. **Validate + build/deploy** as usual. Test with an MCP client (or curl) against \`http://localhost:8300/mcp\` — \`tools/list\` should return the defined tools.

Server name rules (enforced by the VS Code form): ≥3 chars, only letters/digits/hyphens/underscores.

Note: users can also create/edit MCP servers via the VS Code UI (Add Artifact → MCP Server, and the MCP Servers node in the MI Project Explorer). When the user just wants one created, generating the two XML files per this recipe is equivalent and shows up in that UI, provided the naming convention is followed.`,

pitfalls: `## MCP Server — Pitfalls

- **Runtime gate**: MCP servers require MI runtime **4.6.0+**. On older runtimes the artifacts deploy but the listener class is unavailable — don't offer the feature for <4.6.0 projects.
- **Broken naming link**: \`mcp.tools.localentry\` must exactly equal the local entry key, and the key must be \`<ServerName>-mcp-config\` (file \`<ServerName>-mcp-config.xml\`). Wrong suffix → server won't appear under "MCP Servers" in the project explorer and the tools form can't load it.
- **Missing connector dependency**: \`mi-inbound-mcp\` absent from pom.xml → deploy-time \`ClassNotFoundException: org.wso2.carbon.inbound.sse.McpInboundListener\`.
- **Port collisions**: another inbound endpoint (or the MI HTTP transport) on the same port prevents startup. The default 8300 is only a convention — always check.
- **CDATA required**: \`<inputSchema>\` content is raw JSON — omit the \`<![CDATA[...]]>\` wrapper and the XML parser may mangle it or validation fails.
- **Don't invent parameters**: auth/TLS parameters are not part of the MCP inbound endpoint's supported set (CORS + keepalive + ports only). Consuming a *remote* MCP server from an MI AI agent is the separate AI-connector \`MCP\` connection (see \`ai-connector-app-development\`), not this artifact.
- **Tool descriptions matter**: agents pick tools by \`<description>\` + schema property descriptions. Vague descriptions → tools never invoked or misused.
- **Sequence result = final payload**: a sequence-backed tool returns whatever the payload is when the sequence ends — end the sequence with the intended response payload (no \`<respond>\` needed; \`<respond>\`/\`<send>\` semantics don't apply to the MCP dispatch path).`,

};

export const MCP_SERVER_REFERENCE_FULL =
    `# WSO2 MI MCP Server Reference (MI 4.6.0+)\n\n` +
    Object.values(MCP_SERVER_REFERENCE_SECTIONS).join('\n\n---\n\n');
