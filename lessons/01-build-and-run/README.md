# Lesson 1: Source Code, Compiler, Executable

## Objective

Distinguish a text source file from the compiler that translates it and the
executable file the operating system runs. No prior C, Rust, or Zig syntax is
assumed.

All three starter programs have the same specification:

- Print `hello, systems` followed by a newline.
- Finish successfully.

Do not edit the programs yet. Their unfamiliar syntax is evidence to inspect,
not something you are expected to understand immediately.

## Predictions

Before running a command, write brief answers in `predictions.md`:

1. Does executing a `.c`, `.rs`, or `.zig` source file directly run it?
2. After compilation, do you expect the source file to be changed?
3. Is the resulting executable likely to be plain readable text or another
   representation?
4. What evidence could distinguish source code from an executable?

There are no trick questions. A prediction is useful even when it is wrong.

## First checkpoint: C

Stop after making the predictions. The first guided session will explain each
part of the C program and these commands before asking you to run them:

```sh
cc -Wall -Wextra -Wpedantic c/main.c -o build/hello-c
./build/hello-c
```

Rust follows after the C checkpoint, and Zig follows after Rust. We will not
run all three mechanically without understanding what each command does.
