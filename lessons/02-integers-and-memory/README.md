# Lesson 2: Integers and Memory

## First checkpoint: C storage

In `c/main.c`, inspect an integer's value, size in bytes, and address.
Complete the TODO by assigning 1000 to the existing variable and printing
the same observations again.

Before running, predict which observations will change within this one run
and explain why. No exact numeric address prediction is needed.

New C syntax:

- `sizeof number` gives the storage size of `number` in C bytes.
- `&number` produces a pointer to `number`: a value identifying its location.
- `(void *)` converts that pointer to the generic pointer type expected by `%p`.
- In `printf`, `%d` prints an `int`, `%zu` prints the size type returned by
  `sizeof`, and `%p` prints a pointer.

After making the prediction, build from the repository root:

```sh
mkdir -p build
cc -Wall -Wextra -Wpedantic lessons/02-integers-and-memory/c/main.c -o build/memory-c
./build/memory-c
```

An address observed in one execution is not a promise about a later execution.
We will compare this experiment in Rust and Zig, then inspect the actual bytes
that represent the integer. Neither byte inspection nor the other language
checkpoints has been completed yet.
