# Systems KV Lab

A hands-on introduction to systems programming through one small persistent
key-value store implemented feature-by-feature in C, Rust, and Zig.

This is a teaching repository, not a race to finish a database. Each feature is
small enough to expose what the computer and operating system are doing, then
compares how the three languages express and constrain that work.

## Learning loop

For each feature:

1. Build a plain-language mental model.
2. Predict observable behavior.
3. Implement and inspect it in C.
4. Implement and inspect it in Rust.
5. Implement and inspect it in Zig.
6. Compare memory, ownership, cleanup, errors, and safety guarantees.
7. Record evidence and questions in `session-doc.md`.

The three programs share a behavioral specification; they are not required to
be line-for-line translations.

## Roadmap

1. Source code, compilers, executables, and exit status
2. Integers, bytes, addresses, and function calls
3. Stack storage and lifetimes
4. Strings as byte sequences
5. Heap allocation and cleanup
6. Structs, alignment, and data layout
7. Growable byte buffers
8. Files, descriptors, and system calls
9. Binary records and defensive decoding
10. Append-only `set` and `get`
11. Startup recovery and an in-memory index
12. Compaction, failure injection, and durability
13. Concurrency and networking, after the foundations are secure

## Layout

- `lessons/` contains matched exercises and specifications.
- `notes/language-comparison.md` is a factual cross-language reference built
  from observed examples.
- `session-doc.md` is the learning and teaching journal.
- `.agents/skills/systems-programming-tutor/` preserves the teaching method for
  future sessions.

Start with `lessons/01-build-and-run/README.md`. Do not worry if every command
and file extension is unfamiliar; lesson 1 assumes exactly that.
