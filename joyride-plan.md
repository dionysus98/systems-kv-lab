# Joyride tutoring implementation plan

## Scope and completion

Deliver a reusable project skill and a working v1 memory snapshot tool for C
through Joyride/nREPL. Capture real debugger observations before and after a
learner-controlled step; compare value, size, address, and bytes. Support source
navigation, clear stale-state labels, and complete cleanup. Do not edit learner
code or automatically compile, launch, or step while using the tool.

Later layout diagrams, lifetime annotations, Rust/Zig expression support, and
persistence views are extension ideas, not v1 completion requirements.

Delete this plan only after the implementation, documentation, meaningful tests,
and a real debugger integration check pass. Preserve any unresolved limitation
in the skill. Ask the learner for visual feedback on the resulting UI.

## Tasks

- [x] Inspect existing scripts, Joyride skills, nREPL, and local debugger tools.
- [x] Create project skill: connection, API patterns, tutoring uses, evidence
  boundaries, commands, lifecycle, limitations, and future extension guidance.
- [x] Implement pure snapshot comparison and safe rendering helpers.
- [x] Implement a user-invoked panel, C expression capture from a paused VS Code
  debugger, bounded byte inspection, source navigation, and cleanup.
- [x] Add a workspace launcher without changing user activation or keybindings.
- [x] Verify pure behavior, no-debug/running/error states, and repeated loading.
- [x] Build an isolated diagnostic fixture under build/ and verify real snapshots,
  a learner-like step, changed-byte highlighting, and session termination labels.
- [ ] Open the verified panel, document usage and limitations, validate the skill,
  review the diff, remove this plan, and commit the coherent tooling checkpoint.

## Implementation decisions and evidence

- Joyride 0.0.85; nREPL connection verified at the user-supplied port 35937.
- C compiler and GDB available; VS Code C/C++ extension installed.
- Port files and generated executables are local runtime artifacts, not docs.
- Use the debugger as the source of observations. Explanatory annotations must
  never be presented as measured runtime data or proof of language guarantees.
- Pure suite: 5 tests, 31 assertions, no failures/errors.
- Integration: passed all 10 checks. Captured 42 -> 1000, unchanged size/address,
  changed byte offsets 0 and 1; bounded an 80-byte array to 64 bytes. Verified
  running-state rejection, invalid-object error, source navigation, ended state,
  reinstallation, and resource disposal. No active debugger left behind.
- Reliable event tracking requires installing the panel before launching the
  debug session. This constraint and C-only single-threaded scope are documented.
- Workspace-script launcher verified: panel opened, six owned subscriptions,
  no active debugger. Skill validator and whitespace checks pass. Remaining:
  final diff review, checkpoint commit, plan removal, and human-facing demo.
