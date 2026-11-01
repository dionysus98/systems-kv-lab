# Learning checkpoints

Progression rules live in the [tutor skill](.agents/skills/systems-programming-tutor/SKILL.md).
Detailed lesson observations live in [session-doc.md](session-doc.md).
This record starts at the current feature; earlier lesson evidence remains in
the journal, without retroactively assigning new completion requirements.

## 03-c-fixed-store

- Language: C
- Exercise: [Fixed-capacity store](lessons/03-fixed-store/README.md)
- Implementation: [main.c](lessons/03-fixed-store/c/main.c)
- Outcome: a four-entry integer store supporting insertion, replacement,
  lookup, and explicit missing-key and capacity results.
- Learning target: modify caller-owned storage through pointers; distinguish
  occupied entries from array capacity and respect array bounds.
- Stage: `implementing`
- Next learner action: define `Entry` and `Store`, initialize an empty store
  in `main`, and implement `store_set`. Submit the attempt for review before
  adding lookup.

### Acceptance criteria and evidence

| Criterion | Evidence status | Source or remaining evidence |
| --- | --- | --- |
| Empty lookup fails and preserves output initialized to 99. | pending | Exercise check 1. |
| Insert key 10 with value 100 and retrieve it. | pending | Exercise check 2. |
| Replace key 10 with 200 without increasing count. | pending | Exercise check 3. |
| Fill four slots, including key zero and a negative value. | pending | Exercise check 4. |
| Reject a fifth distinct key, preserving count and entries. | pending | Exercise check 5. |
| Replace a key while full, preserving count. | pending | Exercise check 6. |
| Missing lookup in a full store preserves output. | pending | Exercise check 7. |
| Build with the exercise's C17 warning flags and run the checks successfully. | pending | Record command, diagnostics, and observed result. |
| Explain why a store pointer permits caller-visible mutation, how lookup writes its output, and why only indices below count are occupied. | pending | Learner explanation grounded in their implementation or memory inspection. |
| Apply the memory model in a small variation. | pending | After the base feature works, choose and record a bounded variation and evidence of the learner's implementation or repair. |

### Starting evidence

- Source inspection on 2026-11-01: `main.c` contains only the exercise task
  comment. No implementation or execution evidence exists for this feature.
- The tutor introduced `store->count` and searching before rejecting a full
  store: `explained by tutor`. Learner understanding remains unassessed.

## 03-rust-fixed-store

- Language: Rust
- Stage: `queued`
- Outcome: implement the same observable store behavior, then examine mutation,
  borrowing, and bounds guarantees.
- Next action: after the C checkpoint is complete, prepare the Rust exercise
  and its criteria before the learner starts.

## 03-zig-fixed-store

- Language: Zig
- Stage: `queued`
- Outcome: implement the same observable store behavior, then compare pointer
  use, bounds checks, and error reporting across the three languages.
- Next action: after the Rust checkpoint is complete, prepare the Zig exercise
  and its criteria before the learner starts.

## Plan changes

- 2026-11-01: added explicit stages and evidence criteria at the learner's
  request for more deterministic progression. Preserved the current C task and
  the C → Rust → Zig feature order. No new learner progress is claimed.
