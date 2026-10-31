#include "BluetoothSerial.h"
#include "DHT.h"

#define DHTPIN 4
#define DHTTYPE DHT11
#define SOIL_PIN 34

BluetoothSerial SerialBT;
DHT dht(DHTPIN, DHTTYPE);

void setup() {
  Serial.begin(115200);

  // This is the Bluetooth name your Android app searches for.
  SerialBT.begin("AgroNode");

  dht.begin();

  Serial.println("AgroNode iniciado.");
}

void loop() {
  float temperature = dht.readTemperature();
  float airHumidity = dht.readHumidity();

  int rawSoil = analogRead(SOIL_PIN);

  // IMPORTANT:
  // These values are approximate. You may need to calibrate them.
  // Put the sensor in dry soil and wet soil, read rawSoil, then adjust these numbers.
  int soilHumidity = map(rawSoil, 3000, 1200, 0, 100);
  soilHumidity = constrain(soilHumidity, 0, 100);

  if (isnan(temperature) || isnan(airHumidity)) {
    Serial.println("Error leyendo DHT11.");
    delay(2000);
    return;
  }

  // The Android app expects this exact format:
  // T:25.5,H:68.0,S:43
  String data = "T:" + String(temperature, 1) +
                ",H:" + String(airHumidity, 1) +
                ",S:" + String(soilHumidity);

  Serial.println(data);
  SerialBT.println(data);

  delay(2000);
}
