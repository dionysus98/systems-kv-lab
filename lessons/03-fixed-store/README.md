# A Fixed-Capacity In-Memory Store

Build the first useful store in C, then compare its implementation in Rust
and Zig. Start with integer keys and values and capacity for four entries.
Use an array and a linear search; no heap allocation or file I/O yet.

## C implementation task

Write your implementation in `c/main.c`.

- Define `struct Entry` containing an integer key and an integer value.
- Define `struct Store` containing an array of four entries and a count of
  occupied entries. Only entries below the count belong to the store.
- Initialize an empty store in `main`.
- Implement these functions above `main` (the declarations below specify
  their interface, not their bodies):

```c
bool store_set(struct Store *store, int key, int value);
bool store_get(const struct Store *store, int key, int *out_value);
```

`store_set` returns true on success. A new key takes an available slot;
an existing key replaces its value without increasing the count. If a new
key cannot fit, return false and leave the store unchanged. Replacement must
still succeed when the store is full.

`store_get` returns true and writes the found value through `out_value`.
For a missing key, return false and leave `*out_value` unchanged. All integer
keys and values, including zero and negatives, are valid. Assume callers pass
valid non-null pointers for this exercise.

## Syntax reference

These fragments illustrate C constructs; they are not a store implementation.

```c
#include <stdbool.h>  // bool, true, false
#include <stddef.h>   // size_t: unsigned type for sizes and counts
#include <stdio.h>    // printf

struct Point {
    int x;
    int y;
};                      // a struct definition ends with a semicolon

struct Point points[3] = {0}; // fixed array; zero-initialize its elements
struct Point p = {.x = 2, .y = 5};
struct Point *ptr = &p;
p.x = 7;                // access a field on a struct value
ptr->y = 9;             // access a field through a pointer: (*ptr).y
points[0] = p;          // struct assignment copies the field values

for (size_t i = 0; i < 3; ++i) {
    printf("%d\n", points[i].x);
}
```

An array is a fixed sequence of elements stored consecutively. C does not
automatically check that an index is within bounds. A struct groups named
fields in one object; its layout can include unused padding bytes.

Function parameters are passed by value. Passing `struct Store *` copies a
pointer so the function can modify the caller's store through that pointer.
`const struct Store *` permits reading the store through that pointer but
prevents modifying it through that pointer. `int *out_value` lets a function
write an integer into storage supplied by the caller, using `*out_value = ...`.
This separates lookup success from the retrieved value, so every integer value
can be stored without reserving a special missing-value sentinel.

`struct Store store = {0};` initializes all fields to zero. Call functions
with `&store`; for lookup, declare an integer result variable and pass its
address too. Use `%zu` to print the count if you choose `size_t` for it.

## Observable checks

Demonstrate these in `main`, printing results clearly or using assertions
from `<assert.h>` (`assert(condition)` stops execution if the condition fails):

1. A lookup in an empty store fails and leaves an output initialized to 99
   unchanged.
2. Set key 10 to value 100; get key 10 and receive 100.
3. Set key 10 to value 200; get 200 and verify the count remains one.
4. Fill the remaining slots, including key zero and a negative value.
5. Insert a fifth distinct key: fail, preserve the count and existing entries.
6. Replace key 10 while full: succeed and preserve the count.
7. Look up a missing key in the full store: fail without changing the output.

## Build and inspect

Run from the repository root:

```sh
mkdir -p build
cc -std=c17 -Wall -Wextra -Wpedantic -g lessons/03-fixed-store/c/main.c -o build/store-c
./build/store-c
```

After the behavior works, we will inspect the array's layout, explain why the
store pointer is needed, and examine what goes wrong with an invalid index or
a pointer to storage whose lifetime has ended. Then implement the same store
in Rust and Zig and compare the guarantees each language provides.
