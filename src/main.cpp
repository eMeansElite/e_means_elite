#include <Arduino.h>
void setup() {
    pinMode(LED_BUILTIN, OUTPUT);
// write your initialization code here
}

void loop() {
    digitalWrite(LED_BUILTIN, 1);
    delay(100);
    digitalWrite(LED_BUILTIN, 0);
    delay(100);
// write your code here
}