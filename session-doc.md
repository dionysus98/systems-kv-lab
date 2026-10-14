# Systems Programming Learning Journal

Lesson attempts, predictions, explanations, observed results, and pending work.
Only demonstrated understanding is recorded as learned.

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

### C checkpoint evidence

- The learner correctly predicted the exact visible output, including its
  trailing newline, and identified `./` as relative to the current directory.
- Building the unchanged source with warnings enabled produced no diagnostics.
- Running the executable printed `hello, systems` and returned exit status 0.
- Inspection identified `c/main.c` as 85 bytes of ASCII text without execute
  permission and `build/hello-c` as a 15,960-byte executable ELF file with
  execute permission. Their first 16 bytes also differed visibly.

### C checkpoint explanation

- The learner correctly distinguished `main.c` as C source text, `cc` as the
  program that compiles it, and `build/hello-c` as the resulting executable.
- The learner correctly explained exit status 0 as successful completion.
- The learner described standard output as the CLI. Refine this to a byte
  stream that happened to be connected to the terminal for this run, but can
  also be redirected to a file or piped to another program.

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

### Source-versus-executable checkpoint

- The learner correctly predicted that editing `c/main.c` without compiling
  would leave the existing executable printing the old greeting.
- Changing the source to `hello, rebuilt systems` and running the old binary
  still printed `hello, systems`. Rebuilding with warnings enabled then made
  the executable print the new greeting. This directly demonstrated that the
  executable is a separate artifact produced at compile time.

### Lesson 1 retrospective

- The learner explained the pipeline as source code being compiled by the
  language-specific compiler into a native executable, which the operating
  system runs as a process.
- The learner described the shell as the interface that launched `hello-c` and
  connected its output to the terminal. Refine this by separating the shell
  from the terminal: the shell parsed `./build/hello-c`, asked the OS to start
  the executable, and waited for its exit; the terminal displayed the bytes
  written to standard output. Redirection showed that the output stream need
  not be connected to the terminal.
- The learner completed the observable objectives for lesson 1.

## Session 3 — C integer storage (2026-10-14)

### C integer storage checkpoint

- The learner asked about the cast for `%p` and the role of `void` and `*`.
  Explained pointer type syntax versus dereferencing, generic object pointers,
  and why variadic arguments do not implicitly convert `int *` to `void *`.
  These explanations alone do not establish mastery of pointers.
- The learner implemented assignment of 1000 and a second print, then restored
  `(void *)&number` in both calls after review.
- They correctly predicted that only the value would change: size belongs to
  the `int` type, and the same variable retains its location during this run.
- Compilation with `-Wall -Wextra -Wpedantic` emitted no diagnostics. Execution
  exited with status 0 and printed values 42 and 1000, each with size 4 and
  address `0x7ffeeb51c864`. This size is an observation on this implementation,
  not a universal C guarantee.

### Rust integer storage checkpoint

- The learner's Rust source assigns 1000 and prints the same observations again.
- During the requested commit checkpoint, `rustc -D warnings` compiled it without
  diagnostics. Execution exited successfully: values 42 and 1000, size 4 for
  both, and the same address within the run.
- No separate Rust prediction or explanation was recorded; this verifies the
  implementation without claiming mastery of references or borrowing.

## Current checkpoint — Fixed-capacity C store

- Exercise: `lessons/03-fixed-store/README.md`.
- Task: implement a four-entry integer store with set/get, replacement,
  missing-key behavior, and capacity errors in `lessons/03-fixed-store/c/main.c`.
- The implementation file was prepared with only a task comment. No learner
  attempt or test result has been reviewed yet.
- Next: learner defines Entry and Store and implements set, then get and the
  acceptance checks. Compare the completed feature in Rust and Zig afterward.
- The earlier Rust integer assignment drill has been superseded by this task.
- Actual byte representation, memory lifetime failures, and heap allocation
  remain untested.
