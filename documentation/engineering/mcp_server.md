# MCP Map Server

The MCP map server exposes a minimal Map Control Protocol (MCP) endpoint. It accepts map files uploaded over JSON‑RPC, applies a request specific configuration, and returns the processed map text.

Currently the only supported configuration option is `map-size`. The field contains an `x` and `y` dimension that replace any existing `#mapsize` directive in the uploaded map.

The implementation lives in `apps/src/main/scala/com/crib/bills/dom6maps/apps/McpMapServer.scala` and demonstrates basic integration with the `mcp-server` library.
