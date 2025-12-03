#include "ble_spi_conf.h"
// Ten include powyżej coś nie działa, i trzeba samemu sobie wsadzić
#define BNRG2A1_SPI_CLOCK_D3 0
// Na góre pliku: .pio/libdeps/nucleo_l152re/STM32duinoBLE/src/utility/HCISpiTransport.cpp
// żeby działało
// :)
#include <STM32duinoBLE.h>
#include <Arduino.h>
#include <Wire.h>
#include <TMP117.h>
#include <Adafruit_HDC302x.h>

uint8_t TMP_ADDR_GND = 0x48; // 1001000
uint8_t TMP_ADDR_VCC = 0x49; // 1001001
uint8_t TMP_ADDR_SDA = 0x4A; // 1001010
uint8_t TMP_ADDR_SCL = 0x4B; // 1001011
uint8_t TMP_ADDR = TMP_ADDR_GND;
TMP117 tmp(TMP_ADDR);

uint8_t HDC_ADDR_GND = 0x44;
uint8_t HDC_ADDR = HDC_ADDR_GND;
Adafruit_HDC302x hdc = Adafruit_HDC302x();

BLEService sensorsService("a61c8642-2e46-4d1d-2137-f77d8adb5e41");
BLEDoubleCharacteristic charTemp("0000", BLERead | BLEBroadcast | BLENotify);
BLEDoubleCharacteristic charHumid("0001", BLERead | BLEBroadcast | BLENotify);

void newTempEmpty() {
}

// ===== Funkcje testowe =====
// To jest zestaw funkcji, które inicjalizują czujniki, i zwracają true jeśli dany czujnik działa
//
// Towarzyszą im wszystkim flagi useX, które można ustawić w setup(), żeby dalej w kodzie nie używać niepodłączonych
// czujników

bool useThermometer = false;

bool thermometerOk() {
    tmp.init(newTempEmpty);
    return tmp.readConfig() != 65535;
}


bool useHumidity = false;

bool humidityOk() {
    return hdc.begin(0x44, &Wire);
}

// ===== Koniec funkcji testowych =====

// ===== Funkcje odczytu =====
// Te funkcje upraszczają cały proces odczytu z czujnika do maksymalnie prostej wartości double
//
// Może być tak, że duplikujemy w ten sposób odczyt - przykładowo, czujnik HDC za jednym zamachem odczytuje
// wilgotność i temperature. Rozdzielając te wartości na dwie funkcje, być może marnujemy troche czasu, ale znacząco
// ujednolicamy i upraszczamy kod w dalszym użyciu.

double getTemperature() {
    return tmp.getTemperature();
}

double getHumidity() {
    double temp = 0.0;
    double RH = 0.0;
    hdc.readTemperatureHumidityOnDemand(temp, RH, TRIGGERMODE_LP0);
    return RH;
}

// ===== Koniec funkcji odczytu =====


void setup() {
    Serial.begin(115200);
    Wire.begin();
    delay(500);

    // Sprawdzenie bluetooth
    if (!BLE.begin()) {
        while (true) {
            Serial.println("failed to initialize BLE!");
            delay(1000);
        }
    }
    charTemp.writeValue(2137);
    charHumid.writeValue(2137);
    sensorsService.addCharacteristic(charTemp);
    sensorsService.addCharacteristic(charHumid);
    BLE.addService(sensorsService);
    BLE.setAdvertisedService(sensorsService);
    BLE.setLocalName("GekoSense");
    BLE.advertise();

    // Sprawdzenie termometru
    if (thermometerOk()) {
        useThermometer = true;
        Serial.println("Termometr działa ✅");
    } else {
        Serial.println("Termometr nie działa ❌");
    }

    // Sprawdzenie czujnika wilgotności
    if (humidityOk()) {
        useHumidity = true;
        Serial.println("Wilogtność działa ✅");
    } else {
        Serial.println("Wilgotnośc nie działa ❌");
    }

    delay(1000);
}

unsigned long long lastCheck = 0;

void loop() {
    if (millis() - lastCheck > 5000) {
        lastCheck = millis();
        if (useThermometer) {
            double temp = getTemperature();
            Serial.print("Temperatura: ");
            Serial.print(temp);
            Serial.println("°C");
            charTemp.writeValue(temp);
        }
        if (useHumidity) {
            double humid = getHumidity();
            Serial.print("Wilgotność: ");
            Serial.print(humid);
            Serial.println("%");
            charHumid.writeValue(humid);
        }
    }
    BLE.poll();
}
