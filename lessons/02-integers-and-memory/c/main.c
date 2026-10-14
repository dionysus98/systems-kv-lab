#include <stdio.h>

int main(void)
{
    int number = 42;

    printf("value=%d size=%zu address=%p\n",
           number, sizeof number, (void *)&number);

    number = 1000;
    printf("value=%d size=%zu address=%p\n",
           number, sizeof number, (void *)&number);

    return 0;
}
