# Learning checkpoints

Follow the [tutor skill](.agents/skills/systems-programming-tutor/SKILL.md).
Background belongs in `learner-profile.md`; detailed observations belong in
`session-doc.md`. No previous learner's evidence applies to this workspace.

## 01-c-build-and-run

- Language: C
- Exercise: [Source, compiler, executable](lessons/01-build-and-run/README.md)
- Implementation: [main.c](lessons/01-build-and-run/c/main.c)
- Outcome: build and run a greeting program, distinguishing its source,
  compiler, executable, and running process.
- Learning target: compilation produces a separate executable; output is a
  byte stream and successful completion has exit status zero.
- Stage: `queued`
- Assistance: no attempt yet.
- Next learner action: introduce your background and goals, then inspect the
  C starter with the tutor and discuss what compilation will produce.

### Acceptance criteria and evidence

| Criterion | Evidence status | Remaining evidence |
| --- | --- | --- |
| Compile with warnings enabled, run the program, observe the greeting and status zero. | pending | Record the actual build command, diagnostics, output, and exit status. |
| Distinguish source text from the separate executable using inspection. | pending | Record file observations and whether compilation changed the source. |
| Explain the roles of source, compiler, executable, and process, and how output reaches the terminal. | pending | Learner explanation grounded in the run. |
| Apply the model to a bounded variation. | pending | Use source editing and rebuilding, or output redirection; record the learner's reasoning and observed result. |

## 01-rust-build-and-run

- Language: Rust
- Stage: `queued`
- Outcome: build and run the same greeting; identify meaningful differences
  in the toolchain and syntax.
- Next action: after C, define criteria using the shared lesson specification,
  reusing demonstrated concepts rather than repeating trivial drills.

## 01-zig-build-and-run

- Language: Zig
- Stage: `queued`
- Outcome: build and run the same greeting and compare the three toolchains.
- Next action: after Rust, check the installed Zig version and prepare criteria
  and commands compatible with it.

## Later features

After Lesson 1, use the roadmap and observed needs to prepare the next bounded
checkpoint. Introductory integer storage and a C fixed-capacity store are
available under `lessons/`. Define each checkpoint before starting it; do not
interpret later starter code as completed work.

## Plan changes

None. Start at Lesson 1 unless the learner requests a different entry point
and prerequisites are established with evidence.
