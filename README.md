# Systems KV Lab

A hands-on introduction to systems programming through one small persistent
key-value store implemented feature-by-feature in C, Rust, and Zig.

This is a teaching repository, not a race to finish a database. Each feature is
small enough to expose what the computer and operating system are doing, then
compares how the three languages express and constrain that work.

## Start your own journey

This checkout can contain another learner's progress. Create a clean workspace
with Python 3.9 or later:

```sh
python3 scripts/start-learning.py ../my-systems-kv-lab
```

Open the new directory and follow [the new-learner guide](docs/new-learner.md).
The exporter preserves this workspace and gives you starter exercises, a blank
profile, and fresh progress records. No Git history or remote is copied.

The tutor's goal is independent low-level programming: build and explain working
features, diagnose failures, and transfer ideas to new problems. Computer science
concepts such as invariants, complexity, and state machines appear when they help
you reason about the store. The course grows with the learner; later roadmap
items are not all prewritten exercises.

## Learning loop

For each feature:

1. Build a plain-language mental model.
2. Predict behavior when it tests a new memory or systems model.
3. Implement and inspect it in C.
4. Implement and inspect it in Rust.
5. Implement and inspect it in Zig.
6. Compare memory, ownership, cleanup, errors, and safety guarantees.
7. Record checkpoint evidence and revisit concepts through small extensions.

The three programs share a behavioral specification; they are not required to
be line-for-line translations.

## See your progress

Ask the tutor **"Show my progress"** to see a diagram, evidence counts, and your
next action directly in the conversation. The tutor reads the current record
using `python3 scripts/progress.py --chat`; you do not need to run a server.
Clients without diagram rendering get readable text instead.

For the optional browser view, run the local viewer, then open `http://127.0.0.1:8765`:

```sh
python3 scripts/progress.py --serve
```

Refresh the page after a lesson update to see the current checkpoint, next
action, stages, and evidence. Filter by language or expand a checkpoint's
details. The view reads `learning-checkpoints.md`; the tutor maintains that
record. It does not infer mastery or include unrecorded historical progress.

For an offline snapshot, run `python3 scripts/progress.py` and open
`build/progress.html`. Rerun the command to update it. Both modes use only
Python's standard library and work in a fresh learner's workspace.

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
- `learning-checkpoints.md` records stages, evidence, and the next action.
- `learner-profile.md` records the current learner's background and goals.
- `starter/` and `scripts/start-learning.py` provide the clean starting path.
- `.agents/skills/systems-programming-tutor/` preserves the teaching method for
  future sessions.

In your fresh workspace, start with `lessons/01-build-and-run/README.md`.
Do not worry if every command and file extension is unfamiliar; lesson 1
assumes exactly that.
