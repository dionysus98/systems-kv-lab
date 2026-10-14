---
name: joyride-systems
description: Use and extend project Joyride tools for systems-programming tutoring in VS Code, including nREPL access, debugger memory snapshots, and source navigation. Use for editor-assisted lessons or .joyride tooling; learner implementations remain learner-written.
---

# Joyride Systems Tutoring

Joyride runs ClojureScript in VS Code's extension host and exposes the `vscode`
API. This skill is the maintained documentation for our editor teaching tools.
Follow the systems-programming-tutor skill for pedagogy.

## Connection and runtime

Prefer `clj-nrepl-eval` with the user-supplied port or `.joyride/.nrepl-port`.
Never hardcode a previous session's port; ask if neither is available. Read the
clojure-eval skill before first CLI use. Local sockets may require escalation.
Use a quoted heredoc to avoid shell interpolation:

```sh
clj-nrepl-eval -p PORT --timeout 5000 <<'EOF'
(require '["vscode" :as vscode])
(some-> vscode/window.activeTextEditor .-document .-fileName)
EOF
```

- Inspect only relevant project buffers. They can contain unsaved edits; never
  save or replace them implicitly. Keep returned data small.
- Track reusable source under `.joyride/src/`, launchers under `.joyride/scripts/`;
  ignore runtime port files. Require namespaces with `:reload` after editing.
- Require `["vscode" :as vscode]` for editor, diagnostics, webview, and debugger
  APIs, and `[joyride.core :as joyride]` for script lifecycle helpers.
- Use `defn ^:async` and `await` inside functions, not top-level `await`. Define
  functions before callers. Use `defonce` for state and dispose subscriptions,
  commands, and panels before reinstalling. Leave activation/keybindings alone.
- Inspect existing scripts before adding more; prefer built-in dependencies.
- Running a workspace script can change the REPL's current namespace. Inspect
  `*ns*` and use fully qualified vars when following up on async state; do not
  assume aliases or definitions from a prior namespace are still in scope.
- MCP is an alternative if already available. Discover its schema and use a
  stable evaluator identity. In the tested Joyride 0.0.85 setup synchronous MCP
  worked but async result serialization failed. Verify resolved completion;
  a Promise alone is not success.

## Teaching and evidence

Connect source, observations, and explanations: bytes after assignment, pointer
versus struct copies, array layout and bounds, storage lifetime, and eventually
binary records and recovery. The learner writes code, compiles, steps, and
interprets results; prepare views and give targeted guidance. Keep diagnostic
fixtures separate from lesson code and do not reveal exercise implementations.

Label measured data separately from conceptual diagrams or language rules.
Addresses and byte patterns are observations of a run, not portability
guarantees. Rust ownership is static, not runtime metadata. Snapshots are not
reverse execution. Mark stale, truncated, unavailable, and ended-session data;
never invent missing evidence.

## Memory snapshots: v1

Source: `.joyride/src/systems_kv/memory.cljs`.
Launcher: `.joyride/scripts/systems_memory.cljs`.

Run **Joyride: Run Workspace Script**, choose `systems_memory.cljs`, or evaluate:

```clojure
(require '[systems-kv.memory :as memory] :reload)
(memory/install!)
```

Open the tool before starting the C debug session so it can track stops and
continuations. If installed or reloaded during a session, relaunch the debugger
yourself. Pause, enter an object expression such as `number`, and capture Before.
Step yourself; capture After. Compare value, size, address, and changed
bytes. Source buttons reveal capture locations. Clear removes snapshots; Close
or `(memory/dispose!)` removes the panel, commands, and listeners.

The tool accepts identifiers, field access, and constant array indices; arbitrary
calls, assignments, and increments are rejected. Captures use the top frame of
the stopped thread, which may differ from the selected stack frame. At most 64
bytes are read; truncation is explicit. v1 uses C expressions via `cppdbg`/GDB;
Rust/Zig, remote targets, optimized-out objects, and invalid pointers are not
promised. It obtains bytes by evaluating unsigned-character reads in the debugger,
without executing a target function or requiring adapter `readMemory` support.
Errors are shown rather than replaced with invented values. Captures are for
single-threaded learning programs; they are not atomic snapshots of concurrently
mutating memory.

## Verification and extension

Load `[systems-kv.memory-test :as tests]` with `:reload` and run `(tests/run!)`
through nREPL. Verify no-debug/running/error states, reload and disposal, then
real debugger captures. Demonstrate UI changes and ask for visual feedback.

The tracked integration fixture is `.joyride/test/fixtures/memory.c`. Compile
it separately from learner exercises:

```sh
mkdir -p build
cc -std=c17 -Wall -Wextra -Wpedantic -g -O0 .joyride/test/fixtures/memory.c -o build/joyride-memory-fixture
```

End any existing debug session, then invoke the explicit integration test:

```clojure
(require '[systems-kv.integration-test :as integration] :reload)
(-> (integration/run!)
    (.catch #(reset! integration/!result {:status :failed :message (.-message %)})))
:started
```

Read `(select-keys @integration/!result [:status :message :checks :comparison])`
in a later evaluation to inspect resolved completion. End an evaluation that
starts asynchronous work with a simple keyword: the tested nREPL printer cannot
print some Promise objects directly. The integration test launches and steps
only its fixture, removes its breakpoint, and leaves real captured data in the
panel labeled as an ended session. It verifies 10 integration behaviors; pure
tests cover 31 assertions. Verified on Linux with Joyride 0.0.85 and `cppdbg`/GDB.

Add layout, lifetime, or persistence views when a lesson needs them. Identify
the learning question, data source, and debugger capabilities first. Keep usage
and limitations here. Track temporary implementation plans and delete them only
when their acceptance checks pass. Tool setup history does not belong in the
resettable lesson journal.

References: [Joyride](https://github.com/BetterThanTomorrow/joyride),
[Joyride API](https://github.com/BetterThanTomorrow/joyride/blob/master/doc/api.md),
[VS Code API](https://code.visualstudio.com/api/references/vscode-api).
