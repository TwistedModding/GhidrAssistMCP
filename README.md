# GhidrAssistMCP

A powerful Ghidra extension that provides an MCP (Model Context Protocol) server, enabling AI assistants and other tools to interact with Ghidra's reverse engineering capabilities through a standardized API.

## Overview

Version **3.0.0** introduces the explicit tool catalog and application-owned runtime. Clients should refresh their tool definitions after upgrading.

GhidrAssistMCP bridges the gap between AI-powered analysis tools and Ghidra's comprehensive reverse engineering platform. By implementing the Model Context Protocol, this extension allows external AI assistants, automated analysis tools, and custom scripts to seamlessly interact with Ghidra's analysis capabilities.

### Key Features

- **MCP Server Integration**: Full Model Context Protocol server implementation using official SDK
- **Dual HTTP Transports**: Supports SSE and Streamable HTTP transports for maximum client compatibility
- **100 Explicit Tools**: Discoverable operation names with precise input contracts and read/write annotations (93 enabled by default)
- **6 MCP Resource Templates**: Program-specific data resources for program info, functions, strings, imports, exports, and segments
- **7 MCP Prompts**: Pre-built analysis prompts for common reverse engineering tasks
- **Result Caching**: Intelligent caching system to improve performance for repeated queries
- **Async Task Support**: Long-running operations execute asynchronously with task management
- **Multi-Program Support**: Work with multiple open programs simultaneously using `program_name`; use `list_open_programs` project paths to disambiguate duplicate filenames
- **Application Startup**: MCP is available as soon as the project manager opens, before CodeBrowser
- **Shared Views**: Every CodeBrowser shares the runtime configuration and bounded request log
- **Multi-Window Support**: Single MCP server shared across all CodeBrowser windows with intelligent focus tracking
- **Active Context Awareness**: Automatic detection of which binary window is in focus, with context hints in all tool responses
- **Configurable UI**: Easy-to-use interface for managing tools and monitoring activity
- **Real-time Logging**: Track all MCP requests and responses with detailed logging
- **Dynamic Tool Management**: Enable/disable tools individually with persistent settings

## Clients

Shameless self-promotion: [GhidrAssist](https://github.com/jtang613/GhidrAssist) supports GhidrAssistMCP right out of the box.

## Screenshots

![Screenshot](https://github.com/jtang613/GhidrAssistMCP/blob/master/res/Screenshot1.png)
![Screenshot](https://github.com/jtang613/GhidrAssistMCP/blob/master/res/Screenshot2.png)

## Installation

### Prerequisites

- **Ghidra 12.0 or 12.1 Public**, running with Java 25 or newer (extension ZIPs are specific to the selected Ghidra version)
- **An MCP Client (Like GhidrAssist)**

### Binary Release (Recommended)

1. **Download the latest release**:
   - Go to the [Releases page](https://github.com/jtang613/GhidrAssistMCP/releases)
   - Download the latest `.zip` file (e.g., `ghidra_12.1_PUBLIC_20261009_GhidrAssistMCP-3.0.0.zip`)

2. **Install the extension**:
   - In Ghidra: **File → Install Extensions → Add Extension**
   - Select the downloaded ZIP file
   - Restart Ghidra when prompted

3. **Start Ghidra**:
   - The MCP runtime starts in the project manager automatically.
   - CodeBrowsers receive a shared MCP view when opened.
   - Open **Window → GhidrAssistMCP** to configure or disable the server.

### Building from Source

Source builds require Java 25 or newer. The included Gradle wrapper pins the
supported Gradle release; use it instead of a system Gradle installation.

1. **Clone the repository**:

   ```bash
   git clone <repository-url>
   cd GhidrAssistMCP
   ```

2. **Point Gradle at your Ghidra install**:
   - Set `GHIDRA_INSTALL_DIR` (environment variable), or pass `-PGHIDRA_INSTALL_DIR=<path>` when you run Gradle.

3. **Build + install**:

   Ensure Ghidra isn't running and run:

   ```bash
   ./gradlew installExtension
   ```

   This copies the built ZIP into your Ghidra install (`[GHIDRA_INSTALL_DIR]/Extensions/Ghidra`) and extracts it into your Ghidra **user** Extensions folder (replacing any existing extracted copy).

   If you need to override that location, pass `-PGHIDRA_USER_EXTENSIONS_DIR=<path>`.

4. **Restart / verify**:
   - Restart Ghidra.
   - If the plugin doesn't appear, enable it via **File → Configure → Configure Plugins** (search for "GhidrAssistMCP").

## Configuration

### Initial Setup

1. **Open the Control Panel**:
   - Window → GhidrAssistMCP

2. **Configure Server Settings**:
   - **Host**: Default is `localhost`
   - **Port**: Default is `8080`
   - **Enable/Disable**: Toggle the MCP server on/off

### Tool Management

The Configuration tab allows you to:

- **View all public tools** (100 total)
- **Enable/disable individual tools** using checkboxes
- **Save configuration** to persist across sessions
- **Monitor tool status** in real-time

## Headless Mode Quickstart

GhidrAssistMCP can also be started from Ghidra's `analyzeHeadless` launcher. This is useful when you want MCP access to a program loaded in headless Ghidra without opening the CodeBrowser UI.

First, build and install the extension so Ghidra can load the compiled classes and bundled dependencies:

```bash
cd /path/to/GhidrAssistMCP

export GHIDRA_INSTALL_DIR=/path/to/ghidra_12.1_PUBLIC
./gradlew installExtension
```

Set paths for your Ghidra install and extracted user extension. On Linux, Ghidra user extensions usually live under `~/.config/ghidra/<ghidra_profile>/Extensions`:

```bash
export GHIDRA_INSTALL_DIR=/path/to/ghidra_12.1_PUBLIC
export GHIDRA_USER_EXTENSIONS_DIR="$HOME/.config/ghidra/ghidra_12.1_PUBLIC/Extensions"
export GHIDRASSISTMCP_EXT="$GHIDRA_USER_EXTENSIONS_DIR/GhidrAssistMCP"
```

Import a binary and start the MCP server as a headless pre-script:

```bash
"$GHIDRA_INSTALL_DIR/support/analyzeHeadless" /tmp/ghidra-projects McpHeadless \
  -import /path/to/binary \
  -scriptPath "$GHIDRASSISTMCP_EXT/ghidra_scripts" \
  -preScript GAMCPStartServerScript.java "host=127.0.0.1" "port=8080"
```

For a binary that is already imported into the project, use `-process` instead:

```bash
"$GHIDRA_INSTALL_DIR/support/analyzeHeadless" /tmp/ghidra-projects McpHeadless \
  -process binary_name \
  -scriptPath "$GHIDRASSISTMCP_EXT/ghidra_scripts" \
  -preScript GAMCPStartServerScript.java "host=127.0.0.1" "port=8080"
```

To keep a headless MCP session open after analysis completes, run the server as a post-script with wait mode:

```bash
"$GHIDRA_INSTALL_DIR/support/analyzeHeadless" /tmp/ghidra-projects McpHeadless \
  -process binary_name \
  -scriptPath "$GHIDRASSISTMCP_EXT/ghidra_scripts" \
  -postScript GAMCPStartServerScript.java "host=127.0.0.1" "port=8080" "wait=true"
```

MCP clients can connect to:

```text
SSE:             http://127.0.0.1:8080/sse
SSE messages:    http://127.0.0.1:8080/message
Streamable HTTP: http://127.0.0.1:8080/mcp
```

The headless MCP server runs inside the `analyzeHeadless` JVM and uses the loaded `currentProgram`. The server holds a program consumer while it is running so MCP requests do not race against program database closure. Use `wait=true` when you want `analyzeHeadless` to stay open for interactive MCP clients. A harness can also pass `completion_file=/workspace/control/session.complete`; creating that file closes the MCP server cleanly and lets Ghidra save and exit normally.

Disposable static-analysis labs may pass `tool_profile=agent_lab`. This enables sandbox-local program export while arbitrary path import and Ghidra scripts remain disabled because they can expose process secrets or spawn processes. The harness owns artifact imports. Unknown profiles are rejected.

## Available tools

The public catalog contains 100 explicit tools, 93 enabled by default. [The modernization review](docs/modernization-review.md) maps every previous tool to its current name and explains the architecture and remaining improvements. Fetch `tools/list` for authoritative parameter schemas; clients should reconnect after upgrading.

| Purpose | Representative tools |
| --- | --- |
| Project lifecycle | `create_project`, `open_project`, `save_project`, `close_project` |
| Programs and project files | `get_program_info`, `list_open_programs`, `list_project_files`, `open_program`, `close_program` |
| Code and functions | `list_functions`, `search_functions`, `get_function_info`, `get_pseudocode`, `get_disassembly`, `get_pcode`, `get_control_flow_graph` |
| Function identity | `get_function_hash` (SHA-256), `get_function_byte_pattern` (masked prefix), `get_function_prototype` (C declaration) |
| Binary data | `list_imports`, `list_exports`, `list_strings`, `list_memory_blocks`, `list_entry_points`, `get_hexdump` |
| References | `get_xrefs_to`, `get_xrefs_from`, `get_callers`, `get_callees`, `get_call_graph` |
| Comments | `get_comment`, `get_function_comment`, `list_function_comments`, `set_comment`, `set_function_comment`, `remove_comment` |
| Variables and types | `list_function_variables`, `rename_local_variable`, `set_local_variable_type`, `set_function_prototype`, `list_data_types`, `get_data_type`, `set_data_type` |
| Structures and classes | `create_structure`, `set_structure_field`, `infer_structure_from_variable`, `get_structure_field_xrefs`, `list_classes`, `get_class_info` |
| Analysis | `list_analysis_options`, `set_analysis_options`, `analyze_program`, `get_analysis_status`, `cancel_analysis` |
| Jobs | `list_tasks`, `get_task_status`, `cancel_task` |

Names express the operation. Public code readers do not require a `format` selector, and comment/type/script operations do not require an `action` selector. `get_disassembly` reads existing instructions; `disassemble_bytes` changes the listing. Read and write tools have their own MCP annotations.

`import_file`, `export_program`, and the five script tools are disabled by default. Enable the specific operations in the shared Configuration view when needed. Old consolidated names are no longer exposed over HTTP; saved enabled states migrate to their replacement operations.

### Opening a program from the project manager

The MCP server starts when Ghidra's project manager opens, before a CodeBrowser exists. Start with:

```json
{"method":"tools/call","params":{"name":"list_project_files","arguments":{"recursive":true}}}
```

Then open a program using its project path. This launches CodeBrowser when needed:

```json
{"method":"tools/call","params":{"name":"open_program","arguments":{"name":"/binaries/example"}}}
```

### Creating, opening, saving, and closing a project

Project lifecycle tools work while only the project manager is open and return structured project identity/status fields. They require the GUI project manager; the headless launcher manages its own project.

Create a local project in an existing absolute parent directory, with a simple name that omits `.gpr` and `.rep`:

```json
{"method":"tools/call","params":{"name":"create_project","arguments":{"location":"/workspace/projects","name":"review"}}}
```

To open an existing project, use its absolute `.gpr` marker path:

```json
{"method":"tools/call","params":{"name":"open_project","arguments":{"path":"/workspace/projects/review.gpr"}}}
```

Neither operation replaces an active project. Opening the same project again returns `already_open`; close a different active project explicitly before creating or opening another one. `restore_state` defaults to `false`; set it to `true` to restore the saved session's tools and views. Use `open_program` to launch CodeBrowser for a project program.

Save changed open project files and session state:

```json
{"method":"tools/call","params":{"name":"save_project","arguments":{}}}
```

Close the project after saving:

```json
{"method":"tools/call","params":{"name":"close_project","arguments":{"save":true}}}
```

`close_project` defaults to `save=false` and `discard_changes=false`; it refuses unsaved changes unless one is explicitly enabled. These options are mutually exclusive. `discard_changes=true` permits losing unsaved changes. A save failure leaves the project open and reports any files already saved. Closing with no active project returns `already_closed`. Unsaved tool configuration and plugin data may need saving in Ghidra before a safe close, even with `save=true`; explicit discard permits dropping them. Running tool commands and busy project data prevent saving or closing, including closure with discard. The server stays reachable while the project manager remains open, so a client can open the next project using the same connection.

### Reading pseudocode and function identity

```json
{"method":"tools/call","params":{"name":"get_pseudocode","arguments":{"function":"main","program_name":"/binaries/example"}}}
```

Use `get_disassembly` with the same arguments for assembly. `get_function_hash` computes SHA-256 over the function's body bytes; `get_function_byte_pattern` preserves the previous masked-prefix representation. `get_function_prototype` returns the declaration and calling convention.

Long-running operations return a job ID when async execution is enabled. Poll `get_task_status` with `task_id` to retrieve the result, or `cancel_task` to cancel. These tools are an application job API, separate from the optional MCP Tasks extension.

### Program and cursor context

Use `list_open_programs` to obtain exact project paths for `program_name`. Missing or ambiguous explicit targets return an error. Cursor queries refer to the focused CodeBrowser and do not accept a separate program selector. Project operations do not need a loaded program; `import_file.program_name` names the imported program.

Responses carry context information while preserving MCP error flags, structured data, and other content. For example:

```text
[Context] Operating on: example | Active window: another_program

<tool result>
```

## Fork tools

This fork adds tools on top of the upstream catalog, kept under their existing names: `enum`, `equate`, `eval_python` and `save_program`, plus `open_vt_session` / `close_vt_session` for headless Version Tracking. Long-running calls accept `timeout_seconds` (5-3600) where the schema lists it; a watchdog marks overdue tasks `TIMED_OUT` and force-cancels them only when the worker pool is saturated. `eval_python` also accepts `{"sync": true}`.

### `enum` - Enum Data Type Tool

| Action | Description |
| ------ | ----------- |
| `list` | List all enum data types (optional `pattern` filter) |
| `get_info` | Get details of an enum including all named values |
| `create` | Create a new enum with optional `size` (1/2/4/8 bytes) and initial `values` (`"NAME=VALUE,..."`) |
| `add_value` | Add a named value (`value_name`, `value`) to an existing enum |
| `remove_value` | Remove a named value from an existing enum |
| `rename_value` | Rename a value within an enum (`value_name` → `new_name`) |
| `rename` | Rename the enum data type itself |
| `delete` | Delete an enum data type |

Use `category` (e.g. `/MyTypes`) to scope lookups or set the destination for `create`.

### `equate` - Equate Tool

Equates associate symbolic names with constant integer values used in disassembly.

| Action | Description |
| ------ | ----------- |
| `list` | List all equates (optional `pattern` filter) |
| `get_info` | Get details of a specific equate |
| `create` | Create an equate with a `name` and numeric `value` |
| `rename` | Rename an equate (`name` → `new_name`) |
| `delete` | Delete an equate |

### `eval_python` - PyGhidra Evaluator Tool

| Parameter | Description |
| --------- | ----------- |
| `script`  | Python 3 code to execute in Ghidra's context. Requires launching Ghidra with `pyghidra.bat` for Python 3 support (or falls back to Jython 2.7). Globals: `currentProgram`, `currentAddress`, `monitor`, `state`. |
| `sync`    | Optional boolean. Default `false` (async — returns a `task_id`, poll with `get_task_status`). Pass `true` for inline result on quick scripts (under ~2s) to skip the task-id round-trip. |

A `ghidra` helper object is pre-injected with common operations:

| Helper | Description |
| ------ | ----------- |
| `ghidra.decompile(id)` | Decompile a function by name or address |
| `ghidra.get_func(id)` | Return a `Function` object by name or address |
| `ghidra.get_program(name)` | Return an open `Program` object by name |
| `ghidra.get_refs_to(addr)` | List all callers of an address |
| `ghidra.set_comment(addr, text, type)` | Set an EOL/PRE/POST/PLATE comment |
| `ghidra.find_struct(name)` | Return a `Structure` data type object (warning: stringifying dumps the entire field table — use `struct_summary`/`struct_fields` for compact output) |
| `ghidra.struct_summary(name)` | Compact summary: `{name, size, field_count, category}` |
| `ghidra.struct_fields(name, prefix=None)` | Slim field list: `[(offset, name, type, size)]` (optional name-prefix filter) |
| `ghidra.read_bytes(addr, length)` | Read memory as a hex string |
| `ghidra.copy_datatype(name, from_prog, to_prog)` | Copy a struct/enum between open programs |
| `ghidra.list_vt_sessions()` | List open Version Tracking sessions: `[{name, src, dst, match_count}]` |
| `ghidra.get_vt_session(idx=0)` | Return a `VTSession` by index |
| `ghidra.get_vt_matches(session, status)` | Return `[{src, dst, status, similarity, confidence}]`; filter by `ACCEPTED`/`REJECTED`/`AVAILABLE` |
| `ghidra.find_addr_in_version(addr, session)` | Find the accepted destination address for a source address across binary versions |
| `ghidra.accept_vt_match(src_addr, session)` | Accept the first available VT match for a source address |

A `dbg` helper object is also pre-injected for dynamic analysis via the Ghidra Debugger. It owns the whole debugger session lifecycle — attach, inspect, control, detach — from MCP, no GUI needed.

| Helper | Description |
| ------ | ----------- |
| `dbg.attach(pid, offer_title=None, mode='default')` | **Start a session**: attach to a running process by PID (defaults to "dbgeng attach" on Windows). `mode='observe'` uses dbgeng's `DEBUG_ATTACH_NONINVASIVE \| DEBUG_ATTACH_NONINVASIVE_NO_SUSPEND` — the target is never stopped, so it's the safe default for read-only live memory/struct work (`reng.explore`/`find_instances`/`label`/`diff*`); verified live against a running game (CPU time kept climbing throughout, confirming no suspend). `mode='default'` is invasive (suspends) and required for breakpoints/stepping/register writes. Call via `eval_python`'s default async execution, not `sync=True` — the subprocess launch and module resolution can take several seconds. Returns `{ok, offer, trace, image_base, modules_ready, armed_breakpoints, mode}` |
| `dbg.list_attach_offers()` | Attach-capable backends available for `dbg.attach` (e.g. "dbgeng attach", gdb/lldb variants) |
| `dbg.status()` | Summary dict: `{connected, trace, snap, thread, has_live_target, control_mode}`. Not connected? `dbg.attach(<pid>)` |
| `dbg.list_sessions()` | `{open_traces, connections, acceptors}` — open traces and active TraceRMI connections |
| `dbg.server_info()` | `{server_started, server_address}` — TraceRMI inbound server state (rarely needed; attach auto-starts it) |
| `dbg.execute(cmd)` | Run a raw dbgeng/WinDbg command and return its output: `k`, `~`, `lm`, `sxd av`, `.lastevent`, `!analyze -v`, `bp …`. The keystone for engine features Ghidra doesn't surface |
| `dbg.py_execute(python)` | Run raw Python in the ghidradbg backend process (`util`, `util.dbg` in scope); stdout captured |
| `dbg.health()` | Long-session resource report: `{snap_count, max_snap, thread_count, module_count, breakpoint_count, mcp_task_count}` |
| `dbg.execute(cmd)` | Run a raw dbgeng/WinDbg command and return its output: `k`, `~`, `lm`, `sxd av`, `.lastevent`, `!analyze -v`, `bp …`. The keystone for engine features Ghidra doesn't surface |
| `dbg.py_execute(python)` | Run raw Python in the ghidradbg backend process (`util`, `util.dbg` in scope); stdout captured |
| `dbg.health()` | Long-session resource report: `{snap_count, max_snap, thread_count, module_count, breakpoint_count, mcp_task_count}` |
| `dbg.cleanup(keep_snaps=None)` | Delete old trace snapshots via `TraceSnapshot.delete()` to compact the `.DBTrace` file (keeps current snap by default) |
| `dbg.detach()` | Clean target disconnect (interrupt → DETACH → close trace → close TraceRMI connection); keeps the debuggee process alive. Captures `last_event`/`event_registers`/`get_stack` *before* tearing the trace down, so calling it reflexively after an unexpected break doesn't lose that data. Returns `{steps, forensics: {last_event, event_registers, stack}}` |
| `dbg.get_threads()` | List all `TraceThread` objects in the active trace |
| `dbg.get_thread()` / `dbg.get_snap()` | Current thread and snapshot index |
| `dbg.get_event_thread()` | The faulting thread that caused the current break: `{tid, name, key, thread}` (`thread` is the raw `TraceThread` for `get_registers(thread=…)`) — not the parked worker `get_thread()` often returns |
| `dbg.list_modules(name_filter=None)` | Loaded modules with runtime base addresses: `[{name, base, length, path}]` — essential for ASLR conversion |
| `dbg.last_event()` | Exception/event that caused the break: `{code, first_chance, thread_id, description, raw}` (code as int, e.g. `0xc0000409`) |
| `dbg.get_registers(thread, frame, snap)` | Register values as `{name: int}` dict |
| `dbg.event_registers()` | Registers of the faulting thread via dbgeng `r` — reliable on a break (avoids trace-register-space staleness) |
| `dbg.refresh_registers()` | Force live register read from target into trace |
| `dbg.write_register(name, value)` | Write a register value (requires RW control mode) |
| `dbg.read_memory(addr, length, snap)` | Read bytes from trace memory as hex string; accepts EITHER static or live address (auto-translated) |
| `dbg.read_bytes(addr, n)` | Read `n` bytes as a Python `bytes` object (refreshes from target by default) |
| `dbg.read_u64/read_u32(addr)` / `dbg.read_ptr(addr)` / `dbg.read_int(addr, size, signed)` | Typed little-endian scalar reads (int) for chasing pointer chains and fields |
| `dbg.refresh_memory(addr, length)` | Force live memory read from target into trace; accepts either address space |
| `dbg.write_memory(addr, hex_bytes)` | Write bytes to live target memory; accepts either address space |
| `dbg.resume()` / `dbg.interrupt()` | Resume or suspend a single thread (current, or `thread=` given) |
| `dbg.resume_all()` | Resume the WHOLE process (dbgeng `g`) — use this after a `mode='default'` attach, which suspends every thread, not just one; `dbg.resume()` alone leaves the rest frozen |
| `dbg.step_into()` / `dbg.step_over()` / `dbg.step_out()` | Single-step execution |
| `dbg.kill()` | Kill the target process |
| `dbg.list_breakpoints()` | All logical breakpoints: `[{address: '0x...', state, kinds, length, name}]` |
| `dbg.set_breakpoint(addr, length, name, raw=False)` | Logical breakpoint at a static Ghidra address (auto-mapped); returns `{ok, address, state, message}`. `raw=True` issues a dbgeng `bp` on a runtime address — for unmapped/system modules (d3d11.dll, ntdll) not loaded as static programs |
| `dbg.set_raw_breakpoint(rt_addr, kind='e')` | dbgeng `bp` (execute) or `ba` (hardware r/w data) directly on a runtime address, bypassing static mapping; returns `{ok, address, command, output}` |
| `dbg.remove_raw_breakpoint(rt_addr)` | Remove the raw dbgeng breakpoint at a runtime address (pairs with `set_raw_breakpoint`); returns `{ok, address, removed, output}` |
| `dbg.delete_breakpoints(addr)` | Remove all *logical* breakpoints at a static address (also clears program-side `BreakpointMarker` bookmarks); returns `{ok, address, deleted, message}` |
| `dbg.set_exception_filter(code, disposition)` | Control dbgeng exception handling: `break`(sxe)/`second`(sxd)/`notify`(sxn)/`ignore`(sxi). `code` is an int (`0xc0000409`) or filter name (`av`,`sbo`). Returns `{ok, command, output}`. Catch one exception in a storm |
| `dbg.list_exception_filters()` | Current exception/event filter dispositions (dbgeng `sx`) |
| `dbg.ensure_static_mapping()` | Add (or update) the trace ↔ program static mapping required for breakpoints to resolve; called automatically by `set_breakpoint` |
| `dbg.get_stack(thread, snap, walk=True)` | Backtrace. Trace-recorded frames are `{level, pc}`; with `walk` and a live session returns the full parsed dbgeng `k` walk `[{level, stack_ptr, ret_addr, location, module, symbol}]` |

**Address spaces.** `dbg.set_breakpoint` / `delete_breakpoints` take static Ghidra addresses (auto-mapped to live via the trace). The memory methods (`read_memory`, `write_memory`, `refresh_memory`) accept *either* a static or a live address and translate automatically when the value falls in `currentProgram`'s image range. If `read_memory` returns all zeros for a known-mapped region, the page is "cold" — call `refresh_memory(addr, length)` first.

**Multi-version projects.** Program↔module matching defaults to an exact file-name match, so a wrong-version binary is never bound silently. When a program is loaded under a name that differs from the live module (e.g. program `SkyrimSE.1170.exe` vs live module `SkyrimSE.exe`), bind it explicitly with `dbg.use_image("SkyrimSE.exe")` or `reng.use_image("SkyrimSE.exe")` (pass `None` to reset). The override applies to `reng.image_base()`, `ensure_static_mapping`, and memory address translation.

**Raw engine passthrough.** `dbg.execute()` runs any dbgeng/WinDbg command and returns its text output. It is the keystone behind `last_event`, `set_exception_filter`, raw breakpoints, and the `get_stack` backtrace fallback — and is available directly for anything the structured helpers don't cover (`!analyze -v`, `dt`, `u`, `!teb`, etc.).

A `reng` helper object is pre-injected for reverse-engineering workflows. Works with or without a live debugger session; some methods require dynamic state, others operate purely on static analysis.

**ASLR address conversion:**

| Helper | Description |
| ------ | ----------- |
| `reng.image_base()` | Runtime image base of `currentProgram` (canonical, from trace module manager; falls back to thread-name parsing if modules aren't enumerated yet) |
| `reng.to_rt(static_addr)` | Convert static Ghidra address → live runtime address |
| `reng.to_static(rt_addr)` | Convert live runtime address → static Ghidra address |
| `reng.use_image(name)` / `dbg.use_image(name)` | Bind `currentProgram` to a differently-named live module (multi-version projects); `None` resets to exact same-name matching |

**RTTI inspection (MSVC x64):**

| Helper | Description |
| ------ | ----------- |
| `reng.rtti(rt_obj_ptr)` | Decode the C++ class name of a live object pointer (reads `vtable[-8]` → `RTTICompleteObjectLocator` → `TypeDescriptor`) |
| `reng.class_hierarchy(rt_ptr)` | Full inheritance chain: `[{class, offset, index}]` ordered base → derived |
| `reng.vtable_methods(rt_ptr, max=128)` | List vtable slots: `[{slot, rt, static, name, named}]` — `name` comes from Ghidra analysis |
| `reng.scan_vtables(prog=None)` | Scan `.rdata` for all RTTI vtables; returns `{vtable_static_addr: class_name}` (cached per program) |
| `reng.rename_vfuncs(vtable_map=None, dry_run=False)` | Bulk-rename `FUN_*` into `ClassName::vfunc_N` using `scan_vtables` map |

**ReClass.NET-style struct exploration** (backed by Ghidra's `DataTypeManager` — no parallel store; reads from and writes to the existing DTM):

| Helper | Description |
| ------ | ----------- |
| `reng.explore(rt_addr, size=256)` | Field table at a live address; inherited fields tagged `(from ClassName)`; auto-detects vtable / fn-ptrs / sub-object pointers / nulls / floats |
| `reng.follow(rt_addr, offset)` | Dereference the pointer at `[rt_addr+offset]` and `explore()` the sub-object |
| `reng.tree(rt_addr, depth=2, max_ptrs=8, size=128)` | Recursive exploration that follows pointer fields |
| `reng.diff(rt_addr1, rt_addr2, size=256, threshold=0)` | Compare two instances; returns differing 8-byte slots — the "damage one Actor, diff to find health" technique |
| `reng.snapshot_state(rt_addr, size=256, label=None)` | Capture bytes now for later temporal diffing — the single-singleton complement to `diff()` |
| `reng.diff_snapshot(rt_addr, size=256, label=None, threshold=0)` | Diff current memory against a `snapshot_state()` capture — freeze a singleton, trigger an action in-game, see what changed |
| `reng.as_array(rt_addr, offset, count, type_str='f32')` | Read N consecutive typed values (`f32`/`u32`/`ptr`/...) |
| `reng.as_known(rt_addr, offset, struct_name)` | Read an inline embedded sub-struct using a Ghidra DataType name (e.g. `NiPoint3` for an embedded XYZ) |
| `reng.read_struct(rt_addr, {fname: (off, type)})` | Read named fields from an explicit field map |
| `reng.find_type_at(rt_addr, offset, size=8)` | Cross-reference a single field against Ghidra's view (function? known vtable? small int? float?) |

**Struct authoring (C++ inheritance-aware):**

| Helper | Description |
| ------ | ----------- |
| `reng.define_class(name, own_fields, base_class=None, total_size=None, category='/')` | Create/update a struct with proper inheritance — embeds `base_class` at offset 0 so the decompiler shows `this->BaseClass_base.field` |
| `reng.build_hierarchy(rt_ptr)` | Auto-build the full RTTI inheritance chain in the DTM (e.g. `TESForm → TESObjectREFR → Actor`); preserves existing named fields |
| `reng.define_struct(name, fields, category='/')` | Flat struct (no inheritance) — for simple/value types; call with `fields={}` to inspect current definition |
| `reng.patch_struct(name, fields, force=False)` | Targeted field update for partially RE'd projects: overwrites provisional names (`unk*`/`pad*`/`gap*`/`field_*`) freely; `force=True` to overwrite established names |
| `reng.is_provisional(field_name)` | True if name matches `reng.PROVISIONAL_PREFIXES` (configurable per project) |
| `reng.apply_struct(addr, struct_name, is_runtime=False)` | Apply a Ghidra DataType to an address in the Listing view |
| `reng.label(rt_addr, fields, struct_name=None, force=False)` | One-call commit+apply+verify: patches/creates the struct, applies it, and returns the refreshed `explore()` view — collapses the usual 3-round-trip fix loop into one |
| `reng.rename_function(static_addr, new_name, namespace=None)` | Rename a function (USER_DEFINED source; survives re-analysis); optional class namespace |

For managing script files, use the `scripts` MCP tool (list/read/write/run/delete) rather than the analysis prelude.

Typical dynamic-analysis workflow (all via `eval_python` + the `dbg` helper):
1. `dbg.attach(<pid>)` — attach to a running process (`dbg.list_attach_offers()` lists backends); creates the session. Pass `mode='observe'` for read-only work (live struct/singleton exploration) — the target is never suspended, so it's safe against a real/HMD session; the default `mode='default'` is invasive and only needed for breakpoints/stepping/register writes.
2. `dbg.status()` — confirm `connected: True`
3. `dbg.get_registers()` / `dbg.read_memory(addr, n)` / `dbg.set_breakpoint(addr)` to inspect and instrument (breakpoints/writes require `mode='default'`)
4. `dbg.resume_all()` (whole process) or `dbg.resume()` (single thread) / `dbg.interrupt()` to control execution; `dbg.detach()` when done — its return value includes captured `last_event`/registers/stack, taken before the trace is closed

To locate an object instance to inspect, use `reng.find_instances(class_or_vtable)`. For read-only live struct/singleton work end to end: `dbg.attach(pid, mode='observe')` → `reng.explore(addr)`/`find_instances`/`snapshot_state`+`diff_snapshot` → `reng.label(addr, fields)` to commit corrections → `dbg.detach()`.

### `eval_python` caveats

- **Transactions:** each `eval_python` call runs inside one Ghidra transaction. The
  built-in `reng`/`ghidra` write helpers handle this correctly, but in hand-written
  scripts never end a transaction with `commit=False` — a nested abort rolls back the
  *entire* eval, silently reverting every other write in the run. Use one always-committed
  transaction. You also cannot `File → Save` from inside an eval (the active transaction
  holds the lock); call the `save_program` tool afterward instead (or save from the GUI).
- **Live VR targets:** the SteamVR compositor terminates a process that stops submitting
  frames, so any debugger break or long frozen-memory scan can kill the game. Capture fast
  then detach (`dbg.snapshot(addr, len, then='detach')`), or run under the SteamVR **null
  driver** to suspend indefinitely — note the null driver provides an HMD but no
  controllers, so controller-state structs won't populate unless controllers are emulated.

## Shared runtime and views

One application runtime owns the server, backend, configuration, and bounded log. Ghidra requires a plugin adapter and UI provider per window; each is a view of that same runtime. Views replay the shared log when opened, configuration saves refresh every view, and “Clear Shared Log” clears it everywhere. Closing any or all CodeBrowsers leaves the server running while the project manager remains open.

The module initializer attaches the project-manager application plugin, including for existing saved profiles. It skips headless Ghidra, which uses the launcher described above. The application plugin automatically attaches MCP views to CodeBrowsers, and `open_program` uses public Ghidra ToolServices and ProgramManager APIs to launch or reuse a browser.

## MCP protocol support

The extension uses Java SDK 2.0.1. Streamable HTTP at `http://localhost:8080/mcp` supports protocol `2025-11-25` and is the preferred endpoint. Legacy SSE at `/sse` and messages at `/message` use protocol `2024-11-05`. Tool availability changes are delivered without restarting the transport, and resource URI patterns are registered as resource templates.

The latest MCP revision is `2026-07-28`; full support depends on the Java SDK's next major line. See the [official SDK roadmap](https://github.com/modelcontextprotocol/java-sdk/blob/main/ROADMAP.md). The implementation and follow-up plan are detailed in [the review](docs/modernization-review.md).

Six program resource templates expose metadata, functions, strings, imports, exports, and segments. Seven prompts cover function analysis, documentation, data flow, network data, defensive vulnerability review, comparison, and structure inference. Prompt guidance uses the explicit tool catalog.

## Development

### Project Structure

```plaintext
src/main/java/ghidrassistmcp/
├── GhidrAssistMCPInitializer.java # Application bootstrap
├── GhidrAssistMCPApplicationPlugin.java # Project-manager owner
├── GhidrAssistMCPPlugin.java      # CodeBrowser adapter
├── GhidrAssistMCPManager.java     # Singleton coordinator
├── GhidrAssistMCPProvider.java    # UI provider with tabs
├── GhidrAssistMCPServer.java      # MCP server implementation
├── GhidrAssistMCPBackend.java     # Backend tool/resource/prompt management
├── McpBackend.java                # Backend interface
├── McpTool.java                   # Tool interface
├── McpEventListener.java          # Event notification interface
├── cache/                         # Caching system
├── project/                       # GUI project lifecycle controller
├── tasks/                         # Async task system
├── resources/                     # MCP resources
├── prompts/                       # MCP prompts
└── tools/                         # Tool implementations
```

### Adding New Tools

1. **Implement McpTool interface**:

   ```java
   public class MyCustomTool implements McpTool {
       @Override
       public String getName() { return "my_custom_tool"; }

       @Override
       public String getDescription() { return "Description"; }

       @Override
       public boolean isReadOnly() { return true; }

       @Override
       public boolean isLongRunning() { return false; }

       @Override
       public boolean isCacheable() { return true; }

       @Override
       public McpSchema.JsonSchema getInputSchema() { /* ... */ }

       @Override
       public McpSchema.CallToolResult execute(Map<String, Object> arguments, Program program) {
           // Implementation; use isError(true) for operation failures
       }
   }
   ```

2. **Register in backend** (after built-in catalog rationalization):

   ```java
   // In GhidrAssistMCPBackend constructor
   registerTool(new MyCustomTool());
   ```

### Build Commands

```bash
# Clean build
./gradlew clean

# Build extension zip (written to dist/)
./gradlew buildExtension

# Install (extract) extension into the Ghidra user Extensions directory
./gradlew installExtension

# Uninstall (delete extracted directory from the Ghidra user Extensions directory)
./gradlew uninstallExtension

# Build/install with specific Ghidra path (required if GHIDRA_INSTALL_DIR isn't set)
./gradlew -PGHIDRA_INSTALL_DIR=/path/to/ghidra installExtension

# Debug build
./gradlew buildExtension --debug
```

### Dependencies

- **MCP SDK**: `io.modelcontextprotocol.sdk:mcp-core:2.0.1` and `mcp-json-jackson2:2.0.1`
- **Jetty Server**: `12.1.14` with EE11 servlet support (HTTP/SSE transport)
- **Jackson**: `2.21.1` (annotations `2.21`) (JSON processing)
- **Ghidra API**: Bundled with Ghidra installation

The SDK, JSON, HTTP, and supporting runtime libraries are bundled into one jar under private package names. This prevents Ghidra's older Jackson and SLF4J classes, or another extension's dependencies, from overriding the server's selected versions. Historical generated `lib/` jars are excluded from the ZIP.

## Logging

### UI Logging

The **Log** tab provides real-time monitoring:

- **Session Events**: Server start/stop, program changes
- **Tool Requests**: `REQ: tool_name {parameters...}`
- **Tool Responses**: `RES: tool_name {response...}`
- **Error Messages**: Failed operations and diagnostics
- **Cache Hits**: When cached results are returned

### Console Logging

Detailed logging in Ghidra's console:

- Tool registration and initialization
- MCP server lifecycle events
- Async task execution and completion
- Cache statistics
- Database transaction operations
- Error stack traces and debugging information

## Troubleshooting

### Common Issues

#### Server Won't Start

- Check if port 8080 is available
- Verify Ghidra installation path
- Examine console logs for errors

#### Tools Not Appearing

- Ensure plugin is enabled
- Check Configuration tab for tool status
- Verify backend initialization in logs

#### MCP Client Connection Issues

- Confirm server is running (check GhidrAssistMCP window)
- Test connection: `curl http://localhost:8080/sse`
- Check firewall settings

#### Tool Execution Failures

- Verify program is loaded in Ghidra
- Check tool parameters are correct
- Review error messages in Log tab

#### Async Task Issues

- Use `get_task_status` to check task state
- Use `list_tasks` to see all tasks
- Use `cancel_task` if a task is stuck

### Debug Mode

Enable debug logging by adding to Ghidra startup:

```bash
-Dlog4j.logger.ghidrassistmcp=DEBUG
```

## Contributing

1. **Fork the repository**
2. **Create a feature branch**: `git checkout -b feature-name`
3. **Make your changes** with proper tests
4. **Follow code style**: Use existing patterns and conventions
5. **Submit a pull request** with detailed description

### Code Standards

- **Java 25 baseline** for builds and runtime validation
- **Proper exception handling** with meaningful messages
- **Transaction safety** for all database operations
- **Thread safety** for UI operations
- **Comprehensive documentation** for public APIs
- **Explicit operation names** and focused schemas over shared implementations

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- **NSA/Ghidra Team** for the excellent reverse engineering platform
- **Anthropic** for the Model Context Protocol specification

---

**Questions or Issues?**

Please open an issue on the project repository for bug reports, feature requests, or questions about usage and development.
