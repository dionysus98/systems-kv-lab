/* Tool verification fixture; not a learner exercise. */
#include <unistd.h>
int main(void) {
    int number = 42;
    unsigned char payload[80] = {0};
    number = 1000;
    payload[0] = 1;
    sleep(2); /* Leave time to verify that running-state capture is rejected. */
    return number == 1000 && payload[0] == 1 ? 0 : 1;
}
