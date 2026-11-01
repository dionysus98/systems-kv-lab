# Learning Machine Visual Design

## Direction

A sharp, colorful terminal cockpit for deliberate systems learning. It should
feel like a debugger, instrument panel, and workshop—not a status dashboard.

The interface makes the following visible at all times:

- current checkpoint and phase;
- the learner's immediate action;
- why the action matters;
- observed evidence;
- help level and available interventions;
- the next transition.

## Layout

The default Learning Mode is a full-screen view:

```text
┌─ LEARNING MACHINE ───────────────────────────────────────────────┐
│ C FIXED STORE  ·  03  ·  DIAGNOSE                 ● HELP 2/4     │
├───────────────┬──────────────────────────────┬───────────────────┤
│ CURRICULUM    │ ACTIVE CHALLENGE              │ EVIDENCE          │
│               │                              │                   │
│ ✓ Build/run   │ Interpret the first warning   │ BEHAVIOR    3/7   │
│ ✓ Integers    │                              │ EXPLANATION 1/2   │
│ → C store     │ Why: pointers let a function  │ TRANSFER    0/1   │
│ ○ Rust store   │ change caller-owned storage. │                   │
│ ○ Zig store    │                              │ [██████░░░] 60%   │
│               │ Next: inspect the diagnostic │                   │
├───────────────┴──────────────────────────────┴───────────────────┤
│ OBSERVATIONS                                                     │
│ 14:02  BUILD   ✓ compiled with -Wall -Wextra                     │
│ 14:03  TEST    ⚠ store_set failed: count changed unexpectedly   │
│ 14:04  TUTOR   What invariant should count preserve?            │
├──────────────────────────────────────────────────────────────────┤
│ [p] predict  [h] hint  [r] run  [s] submit  [e] explain  [q] quit│
└──────────────────────────────────────────────────────────────────┘
```

## Semantic palette

Use Pi theme tokens rather than hard-coded ANSI escapes. The initial dark theme
maps semantic roles approximately as follows:

| Role | Meaning | Suggested color |
| --- | --- | --- |
| accent | current focus and navigation | bright cyan |
| success | learner-demonstrated evidence | bright green |
| warning | pending attention or ambiguity | bright yellow |
| error | failed check or blocked transition | bright red |
| transfer | application and variation work | bright magenta |
| observation | tool output | blue |
| muted | supporting information | slate gray |
| border | panel structure | dark cyan/gray |

Every status also has text and an icon, so the UI remains usable without color.

## Typography and composition

- One strong title line per panel.
- Uppercase labels for scanning; sentence case for explanations.
- Use blank lines as grouping, not decorative clutter.
- Keep the active challenge visually dominant.
- Keep tool output compact by default; allow expansion for detail.
- Use fixed-width numbers for evidence counts and timestamps.
- Avoid emoji that render inconsistently; prefer stable Unicode symbols.

## Responsive behavior

- Wide terminals: three columns as shown above.
- Medium terminals: curriculum becomes a top strip; challenge and evidence
  remain side by side.
- Narrow terminals: stack panels vertically in this order:
  challenge, next action, evidence, observations, curriculum.
- All lines must be measured and truncated using Pi TUI utilities.
- Every mouse interaction must have a keyboard equivalent.

## Interaction states

The prototype must render these fixtures:

1. `orient`: explain the goal and first learner action.
2. `predict`: collect a prediction before running a tool.
3. `attempt`: learner owns the implementation step.
4. `diagnose`: show observations and request interpretation.
5. `explain`: collect the learner's memory-model explanation.
6. `transfer`: present a small variation.
7. `complete`: celebrate demonstrated progress and show the next feature.
8. `blocked`: explain the missing criterion and offer the smallest useful help.

## Motion and feedback

Keep animation restrained. Use short loader/status transitions for running tools,
then replace them with a durable observation. Do not animate essential meaning.
A successful transition should produce a brief visual confirmation and update the
phase timeline, evidence counts, and next action together.

## Accessibility and trust

- Never encode meaning by color alone.
- Support terminal themes and light/dark appearance.
- Preserve readable contrast.
- Make destructive or filesystem-changing actions explicit.
- Show when the tutor, rather than the learner, performed an action.
- Make evidence provenance expandable: source, command, and observed result.
