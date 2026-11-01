# Learning Machine: End-to-End Plan

## Vision

Turn Pi from a coding agent with tutoring instructions into an explicit,
adaptive learning environment. The learner should always see the current goal,
why the task was selected, what evidence exists, what help is available, and
what happens next.

Pi remains the agent runtime. This project provides the curriculum, learner
model, evidence model, interventions, and learning-oriented interface.

## Product principles

- Preserve learner agency: the learner writes substantive implementations.
- Make hidden tutor state visible.
- Separate behavior, explanation, and transfer evidence.
- Give the smallest useful help before revealing solutions.
- Make every tool action educationally legible.
- Prefer meaningful progress over completion theater.
- Keep the experience warm, curious, and celebratory.
- Work in normal Pi mode as well as Learning Mode.
- Keep durable learning state independent of a single chat transcript.

## Target interaction

```text
orient -> predict -> attempt -> observe -> diagnose -> repair
       -> explain -> transfer -> reflect -> advance
```

Each phase has a visible status, allowed learner actions, evidence required,
and a next-action recommendation. Advancement is blocked only when a required
criterion is genuinely needed; the tutor may continue locally when evidence is
still incomplete.

## Architecture

```text
                   +--------------------+
                   | Pi runtime / model |
                   +----------+---------+
                              |
             +----------------+----------------+
             |                                 |
   +---------v---------+             +---------v---------+
   | Tutor orchestrator|             | Learning TUI      |
   | phase + decisions |             | cockpit + controls|
   +---------+---------+             +---------+---------+
             |                                 |
   +---------v---------------------------------v---------+
   | Structured learning state and event log              |
   | learner profile, checkpoints, evidence, attempts     |
   +-------------------------+-----------------------------+
                             |
              +--------------v--------------+
              | Exercises, checks, tools,   |
              | debugger and visualizers    |
              +-----------------------------+
```

### Components

1. **Curriculum**
   - Exercise specifications and dependency graph.
   - Checkpoint criteria and bounded variations.
   - C -> Rust -> Zig feature ordering.

2. **Learner model**
   - Durable goals, preferences, background, and accessibility needs.
   - Concept-level status: behavior, explanation, transfer, confidence.
   - Assistance history and recurring misconceptions.

3. **Evidence engine**
   - Records predictions, code attempts, diagnostics, test results,
     explanations, repairs, and variations.
   - Uses explicit statuses: pending, tutor-explained, tutor-verified,
     learner-demonstrated.
   - Never infers understanding solely from passing tests.

4. **Tutor orchestrator**
   - Selects the next intervention from the current phase and evidence.
   - Controls graduated hints.
   - Requests predictions, explanations, debugging, or transfer tasks.
   - Produces a concrete next action.

5. **Pi integration**
   - Skills provide teaching policy and domain knowledge.
   - Prompt templates provide repeatable commands.
   - Extensions provide lifecycle hooks, tools, state restoration, and TUI.
   - Pi SDK becomes the future host for a dedicated learning application.

6. **Learning TUI**
   - Curriculum panel.
   - Active challenge panel.
   - Evidence panel.
   - Observation/tool stream.
   - Phase and help-level indicators.
   - Keyboard-first controls with mouse support where useful.

7. **VS Code / Joyride companion**
   - Use Joyride for editor-native debugger and memory visualizations.
   - Keep Pi responsible for lesson orchestration, prompts, evidence policy, and
     progression; keep VS Code responsible for source navigation and live
     debugging.
   - Exchange structured observations rather than scraping rendered HTML.
   - Prefer a shared append-only observation log or explicit nREPL commands.
   - Label snapshots as measured, stale, truncated, unavailable, or ended.

## State model

Use structured state internally, while keeping Markdown records readable and
reviewable during the migration.

```json
{
  "checkpoint": "03-c-fixed-store",
  "phase": "attempt",
  "goal": "modify caller-owned storage through a pointer",
  "helpLevel": 1,
  "criteria": {
    "behavior": "pending",
    "explanation": "pending",
    "transfer": "pending"
  },
  "nextAction": "define Entry and Store, then submit the attempt",
  "lastIntervention": "conceptual_hint"
}
```

Events should be append-only and branch-aware:

```text
PredictionMade
AttemptSubmitted
ToolObservationRecorded
HintGiven
ExplanationSubmitted
CriterionVerified
VariationStarted
CheckpointAdvanced
```

The current state is derived from the active session branch plus durable project
records. Do not reconstruct branch-sensitive state from abandoned branches.

## TUI design

### Persistent normal-mode additions

- Compact footer/status line: checkpoint, phase, and next action.
- `/progress` opens the evidence summary.
- Educational tool renderers show purpose, command, result, and learner move.

### Learning Mode cockpit

A full-screen custom TUI contains:

- **Curriculum:** completed, current, and queued checkpoints.
- **Challenge:** goal, specification, and current learner action.
- **Evidence:** behavior, explanation, transfer, and assistance.
- **Observation stream:** compiler, test, debugger, and filesystem results.
- **Controls:** predict, hint, run, explain, submit, reflect, next.

The initial version should be keyboard-first, narrow-terminal safe, theme-aware,
and usable without a mouse.

## Command surface

- `/learn` — enter or resume Learning Mode.
- `/progress` — show current checkpoint and evidence.
- `/predict` — record a prediction before an experiment.
- `/hint` — receive the next graduated hint.
- `/submit` — mark the current implementation attempt for review.
- `/explain` — start an explanation checkpoint.
- `/reflect` — record a short lesson retrospective.
- `/next` — request the next learner action; never silently skip criteria.
- `/normal` — return to ordinary Pi mode.

## Delivery phases

### Phase 0 — Design baseline

- Approve this architecture and terminology.
- Define the first TUI wireframe and state transitions.
- Keep existing Markdown records as the source of truth.

**Exit:** one agreed checkpoint can be represented from orient through
transfer without inventing evidence.

### Phase 1 — Tutor protocol

- Refine the systems tutor skill around phases, agency, tone, and evidence.
- Add prompt templates for the command surface.
- Add tests for checkpoint parsing and valid state transitions.

**Exit:** Pi can consistently resume the current checkpoint and produce one
bounded learner action.

### Phase 2 — Learning state service

- Introduce typed state and event structures.
- Build Markdown import/export compatibility.
- Persist branch-aware session state through Pi entries.
- Add deterministic validation for stages and evidence.

**Exit:** state survives reload, branching, and resumed sessions without losing
learner evidence.

### Phase 3 — Pi extension MVP

- Add project extension in `.pi/extensions/`.
- Implement `/learn`, `/progress`, `/hint`, `/predict`, `/explain`, and
  `/reflect`.
- Inject current learning state through `before_agent_start`.
- Restore state on `session_start` and session-tree changes.
- Add tool-result annotations and a compact status widget.

**Exit:** an entire C checkpoint can be completed through Pi with visible state
and no manual record editing.

### Phase 4 — Learning Mode TUI

- Build the full-screen cockpit with `ctx.ui.custom()`.
- Add curriculum, challenge, evidence, and observation panels.
- Add keyboard navigation, resize handling, theme support, and narrow-width
  tests.
- Make all controls available without requiring natural-language prompts.

**Exit:** the learner can understand the current lesson state at a glance and
operate the complete lesson loop from the TUI.

### Phase 5 — Learning tools

- Educational compiler/test runner with purpose and expected observation.
- Isolated broken variants for debugging practice.
- Memory layout and pointer visualizations.
- Hex inspection and debugger snapshots.
- Safe failure injection for later persistence exercises.

**Exit:** the harness can create and inspect relevant failures without damaging
learner work.

### Phase 6 — Adaptive tutor

- Add intervention policy based on evidence and assistance history.
- Detect repeated misconceptions and select targeted variations.
- Adjust challenge size and explanation depth.
- Add learner-controlled pace and difficulty preferences.
- Record why an intervention was selected.

**Exit:** two learners with different evidence profiles receive different next
steps while following the same behavioral curriculum.

### Phase 7 — Dedicated application

- Extract the learning state/orchestrator into a reusable package.
- Use the Pi SDK behind a dedicated terminal or web application.
- Add session replay, concept graphs, visual timelines, and accessibility
  options.
- Keep Pi CLI integration as a lightweight mode.

**Exit:** the learning machine is a product that can host multiple curricula,
not only the Systems KV Lab.

## Testing strategy

- Unit tests for state transitions, parsing, evidence rules, and branching.
- Fixture tests for compiler/test observations.
- TUI snapshot tests at narrow, normal, and wide widths.
- Interaction tests for keyboard navigation and cancellation.
- Tutor-policy tests: no unsolicited full solutions, no unsupported mastery
  claims, and every session ends with a concrete next action.
- End-to-end fixtures covering one complete C checkpoint.

## First vertical slice

Do not build the entire platform first. Implement one complete slice for the
current C fixed store:

```text
load checkpoint
 -> show challenge
 -> record prediction
 -> learner edits main.c
 -> run warning-enabled build
 -> display diagnostics
 -> request explanation
 -> run acceptance checks
 -> request a small variation
 -> update evidence and next action
```

This slice should prove the architecture before adding Rust, Zig, memory
visualizations, or a separate application.

## Success criteria

The feature is successful when a learner can:

1. Open Pi and immediately see the current learning goal.
2. Understand why the current task was selected.
3. Write and test their own implementation.
4. See tool results framed as observations rather than opaque automation.
5. Ask for graduated help without receiving an unsolicited solution.
6. See behavior, explanation, and transfer evidence separately.
7. Resume later without losing context.
8. Finish a session feeling oriented, capable, and curious about the next step.
