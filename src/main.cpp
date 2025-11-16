#include <Wire.h>
#include <TMP117.h>

uint8_t ADDR_GND = 0x48; // 1001000
uint8_t ADDR_VCC = 0x49; // 1001001
uint8_t ADDR_SDA = 0x4A; // 1001010
uint8_t ADDR_SCL = 0x4B; // 1001011
uint8_t ADDR = ADDR_GND;
TMP117 tmp(ADDR);

void newTempEmpty() {
}

void setup() {
    Serial.begin(9600);
    Wire.begin();

    tmp.init(newTempEmpty);
}

void loop() {
    Serial.print(tmp.getTemperature());
    Serial.println(" °C");
    // WORKS !!!
    delay(500);
}
