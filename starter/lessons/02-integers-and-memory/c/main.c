#include <stdio.h>

int main(void) {
    int number = 42;

    printf("value=%d size=%zu address=%p\n",
           number, sizeof number, (void *)&number);

    // TODO: Assign 1000 to number, then print the same three observations.

    return 0;
}
