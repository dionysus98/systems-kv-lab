# Systems Programming Learning Journal

This is a living record of demonstrated understanding and teaching quality. It
does not mark an idea learned merely because it was presented.

## Learner context

- The learner is an experienced programmer but a complete beginner in C,
  Rust, Zig, and systems programming.
- Explain new syntax and systems terminology when first introduced.
- Keep exercises small enough that language syntax does not obscure the
  machine-level concept.
- Teach each feature in C, then Rust, then Zig, followed by an explicit
  comparison and discussion.

## Learning goals

- Form a concrete model of memory, program execution, and operating-system I/O.
- Understand how C, Rust, and Zig make different tradeoffs around control,
  safety, ownership, allocation, and errors.
- Use compilers, warnings, sanitizers, debuggers, syscall traces, and byte-level
  inspection deliberately.
- Build a recoverable append-only key-value store in all three languages.
- Explain why an implementation works and identify what could make it fail.

## Working method

1. Introduce one small machine-level concept.
2. Predict before executing.
3. Implement the same observable feature in C, Rust, and Zig.
4. Test the prediction and inspect surprising results.
5. Compare guarantees and tradeoffs.
6. Record evidence, misconceptions, teaching feedback, and the next exercise.

## Session 0 — Curriculum design (2026-08-17)

### Decisions

- Use a tiny persistent key-value store as the motivating project.
- Work feature-by-feature across C, Rust, and Zig instead of completing one
  language before starting another.
- Use Linux as the initial platform.
- Begin with stack-based experiments before heap allocation.
- Delay concurrency until memory, bytes, files, and persistence are understood.
- Compare behavior rather than forcing identical source code.

### Teaching evidence and feedback

- The learner explicitly emphasized being a complete beginner in all three
  languages. Future teaching must not infer language knowledge from general
  programming experience.
- The learner wants discussion of memory management and why these languages
  exist, tied to each implemented feature.

### Environment observation

- C compilers, Rust, GDB, `strace`, and Valgrind are available.
- Zig 0.16.0 is available through the system's asdf-managed setup.
- The initial Zig starter targeted an older standard-library API. It was
  corrected to use the Zig 0.16 `std.Io.File` API and verified independently
  of learner work.

### Next exercise

- Complete lesson 1 predictions, then build and run the C program without
  changing it.
- Explain the distinction between source code, a compiler, and an executable
  in the learner's own words.

## Retrospective prompts

- What can I now explain without relying on language-specific terminology?
- What did I predict, and what evidence confirmed or corrected it?
- Which safety checks came from the language, compiler, tool, or operating
  system?
- Did unfamiliar syntax obscure the systems concept?
- Did the assistant reveal an answer too early or assume missing knowledge?
