# C, Rust, and Zig Comparison Notes

Add claims here only after observing them in an exercise. Keep distinctions
between language rules, compiler behavior, operating-system behavior, and
project conventions explicit.

| Topic | C | Rust | Zig | Evidence |
| --- | --- | --- | --- | --- |
| Build pipeline | `cc` produces `hello-c` | `rustc` produces `hello-rust` | `zig build-exe` produces `hello-zig` | Lesson 1: each compiler produced a separate native executable from text source |
| Successful greeting | Text plus newline; status 0 | Text plus newline; status 0 | Text plus newline; status 0 | All three programs built and ran on Linux; this shared behavior is the lesson specification |
| Stack values | Pending | Pending | Pending | Future lesson |
| Heap allocation | Pending | Pending | Pending | Future lesson |
| Ownership and cleanup | Pending | Pending | Pending | Future lesson |
| Error handling | Pending | Pending | Pending | Future lesson |
