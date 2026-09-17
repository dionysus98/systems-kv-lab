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

## Session 1 — Lesson 1 predictions (2026-08-23)

### Teaching evidence

- The learner predicted that compilation leaves the source unchanged and
  produces a binary rather than plain readable text.
- The learner's first answer conflated source files with the native binaries
  produced from them. The next experiment will distinguish the input source
  from the separate compiler output.
- When asked for evidence distinguishing source and executable, the learner
  restated their conceptual roles rather than proposing an observable test.
  The C checkpoint will compare file type, permissions, and visible contents.

### Next exercise

- Explain the small C program and build command, then ask the learner to
  predict the program output before compiling or running it.

### C checkpoint evidence

- The learner correctly predicted the exact visible output, including its
  trailing newline, and identified `./` as relative to the current directory.
- Building the unchanged source with warnings enabled produced no diagnostics.
- Running the executable printed `hello, systems` and returned exit status 0.
- Inspection identified `c/main.c` as 85 bytes of ASCII text without execute
  permission and `build/hello-c` as a 15,960-byte executable ELF file with
  execute permission. Their first 16 bytes also differed visibly.

### Next exercise

- Have the learner use the observed C evidence to explain source, compiler,
  executable, standard output, and exit status before moving to Rust.

### C checkpoint explanation

- The learner correctly distinguished `main.c` as C source text, `cc` as the
  program that compiles it, and `build/hello-c` as the resulting executable.
- The learner correctly explained exit status 0 as successful completion.
- The learner described standard output as the CLI. Refine this to a byte
  stream that happened to be connected to the terminal for this run, but can
  also be redirected to a file or piped to another program.

### Next exercise

- Begin the Rust checkpoint by explaining only the syntax in `rust/main.rs`
  and the `rustc` build command, then ask the learner to predict the visible
  output, whether the source changes, and what artifacts compilation creates.

## Session 2 — Rust build checkpoint (2026-09-17)

### Teaching evidence

- The learner predicted `hello, systems`, unchanged source, and a separate
  executable named `hello-rust`, connecting this to the C experiment.
- The assistant clarified that `println!` adds a trailing newline, then built
  the existing Rust source with `rustc` without diagnostics.
- Execution printed the predicted text followed by a newline and returned
  exit status 0. Matching SHA-256 hashes before and after compilation confirmed
  the source was unchanged; inspection identified the output as an ELF executable.
- The `file` utility mislabeled the tiny Rust source as C source while correctly
  identifying it as ASCII text. Its language guess is not authoritative.
- Standard output was explained as a byte stream; the learner has not yet
  demonstrated that refinement in their own explanation.

### Redirection checkpoint

- The learner correctly predicted that redirecting standard output would
  leave the terminal empty and put `hello, systems` in `greeting.txt`.
- Running `./build/hello-rust > build/greeting.txt` produced no terminal output
  and exited successfully. Reading the file confirmed the text and a trailing
  newline (byte `0a`). This demonstrates the distinction between standard
  output and the terminal.
- The learner explicitly authorized committing and pushing the journal after
  automatic approval review initially rejected publishing the checkpoint.

### Next exercise

- Introduce the existing Zig starter a little at a time, explaining its explicit
  I/O context and error handling before asking for a build/run prediction.

### Zig checkpoint

- The learner correctly predicted `hello, systems` followed by a newline and
  explained that `try` returns an error if the operation fails.
- The initial build could not write Zig's default global cache in the restricted
  environment. Using `--global-cache-dir /tmp/systems-kv-lab-zig-cache` allowed
  the same source to compile successfully without diagnostics.
- Execution printed the predicted text and newline and returned exit status 0.
  Matching source hashes confirmed compilation left the source unchanged;
  `file` identified `build/hello-zig` as an ELF executable.
- Error propagation was explained and correctly restated, but no failing write
  has been tested. Memory, ownership, and cleanup remain untested.

### Next exercise

- Close lesson 1 with a cross-language comparison and ask whether editing a
  source file alone changes an already-built executable. Use the prediction
  to guide the learner's first source edit and rebuild.

### Source-versus-executable checkpoint

- The learner correctly predicted that editing `c/main.c` without compiling
  would leave the existing executable printing the old greeting.
- Changing the source to `hello, rebuilt systems` and running the old binary
  still printed `hello, systems`. Rebuilding with warnings enabled then made
  the executable print the new greeting. This directly demonstrated that the
  executable is a separate artifact produced at compile time.

### Next exercise

- Ask the learner to summarize the complete lesson 1 build pipeline and identify
  which observed facts came from the source, compiler, executable, shell, and
  operating system before beginning lesson 2.

## Retrospective prompts

- What can I now explain without relying on language-specific terminology?
- What did I predict, and what evidence confirmed or corrected it?
- Which safety checks came from the language, compiler, tool, or operating
  system?
- Did unfamiliar syntax obscure the systems concept?
- Did the assistant reveal an answer too early or assume missing knowledge?
