# AgroNode

**An Android + ESP32 agricultural sensor-monitoring prototype.** The Android app displays temperature, air humidity and approximate soil moisture with live graphs and a demonstrative environmental-risk indicator. It can also run in **demo mode** without hardware. The intended purpose is to detect plagues early, though it can be used for general monitoring. School project.

## Features

- ESP32 firmware samples a DHT11 temperature/humidity sensor and a capacitive analog soil-moisture sensor every 2 seconds.
- Bluetooth **Classic SPP** sends CSV-like text telemetry to a previously paired Android phone.
- Android screen displays current values, recent readings (up to 30 per graph) and a simple risk label.
- Demo mode produces simulated readings without an ESP32.
- A separately testable JVM parser rejects incomplete, repeated, out-of-range and non-finite telemetry.

## Repository structure

| Path | Purpose |
| --- | --- |
| `app/src/main/java/com/example/agronode/` | Android activity, graphs and protocol parser |
| `app/src/main/res/` | Android interface and UI resources |
| `esp32_code/AgroNodeESP32/AgroNodeESP32.ino` | ESP32 Arduino firmware |
| `tests/` | Pure-Java parser checks |
| `scripts/test-protocol.sh` | Run parser checks without an Android SDK |
| `gradlew`, `gradle/wrapper/` | Pinned Android build launcher |

## Hardware and wiring

A Bluetooth Classic-capable ESP32 is required. **BluetoothSerial may not work on ESP32-C3/S3 variants.** Use a 3.3 V-compatible DHT11 module and a 3.3 V analog capacitive soil sensor.

| Sensor | Sensor pin | ESP32 pin |
| --- | --- | --- |
| DHT11 | VCC / GND / DATA | 3.3V / GND / GPIO 4 |
| Capacitive soil sensor | VCC / GND / AOUT | 3.3V / GND / GPIO 34 |

Ensure your DHT11 module has the required pull-up resistor, if one is not built in. Do not send more than 3.3 V to the ESP32 analog input.

## Flash the firmware

1. Install Arduino IDE and the **Espressif ESP32 board support package**.
2. Install **Adafruit DHT sensor library** and **Adafruit Unified Sensor**.
3. Open `esp32_code/AgroNodeESP32/AgroNodeESP32.ino` and select your supported ESP32 board and port.
4. Adjust the `map(rawSoil, 3000, 1200, 0, 100)` calibration endpoints for your own sensor and soil conditions.
5. Upload the sketch, power the board and pair your Android phone with the Bluetooth device **AgroNode** through system settings.

The serial/Bluetooth frame format is `T:25.5,H:68.0,S:43` (temperature in °C and humidity values in percentages). All three fields are required.

## Build and run the Android app

Prerequisites: Android Studio with Android SDK 34, JDK 17 or 21, and an Android phone with Bluetooth Classic support (Android 6.0+, API 23). The project uses Android Gradle Plugin 8.3.2 and Gradle 8.5.

1. Open this repository's **root** in Android Studio; let Gradle sync and install SDK 34 if prompted.
2. Run the `app` configuration on a physical phone (or test **demo mode** on a suitable emulator).
3. For a hardware connection, enable Bluetooth, pair the ESP32 first, press **Conectar**, and grant the Bluetooth permission when prompted (Android 12+).
4. Alternatively, press **Modo demo** to see generated readings without a sensor.

Command-line build, with the Android SDK installed:

```sh
./gradlew :app:assembleDebug
```

Run the pure-Java telemetry tests without an Android SDK:

```sh
./scripts/test-protocol.sh
```

## AI usage

This is **AI-ASSISTED**, primarily in visual design. Though it is verified to work with real components as I have properly connected and tested it in real life. 

