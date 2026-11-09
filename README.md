# SiliGuide

SiliGuide is an Android app that helps greenhouse growers check the health of **Siling Labuyo (chili) plants**.

You take a photo of a leaf. An on-device AI model finds the leaf symptom or condition. The app then combines that result with live soil and air readings from a SiliGuide ESP32 sensor device, and suggests the likely condition and what to do about it.

The app works **offline first**. Cloud sync is optional.

---

## Features

- **Leaf analysis on the phone**
  - A YOLO instance-segmentation model (LiteRT / TFLite) finds and outlines each leaf symptom.
  - Classes: `curling`, `healthy`, `spots`, `yellowing`.
  - Blur check, crop and zoom tool, and overlay of the detected areas.
- **Sensor-aware diagnosis**
  - A rule engine combines the leaf result with 9 sensor readings: soil moisture, soil temperature, pH, EC, nitrogen, phosphorus, potassium, light and air temperature.
  - Each reading is rated LOW / NORMAL / HIGH against chili-specific ranges.
  - Without a sensor, the app gives a camera-only result.
- **IoT sensor device (ESP32)**
  - Three ways to connect: **Hotspot (AP)**, **Bluetooth**, and **LAN** (the device joins your home Wi-Fi).
  - Live readings on the device screen, refreshed every few seconds. Last known readings are shown when the device is offline.
  - Secure pairing: each device is claimed with its own pairing token, and sensor data is encrypted with AES-256-GCM.
  - Several devices per account. Each device is identified by its hardware MAC.
- **Plants and history**
  - Add plants, link each plant to a sensor device, and keep a history of past analyses with their photos and readings.
- **Accounts and sync**
  - Sign in with email OTP or Google, or use the app as a guest.
  - Signed-in data syncs to Supabase in the background. A guest's data moves to the new account on sign-up.

---

## How it works

```
 ┌──────────────┐   photo    ┌──────────────────┐
 │    Camera    │──────────▶│  YOLO-seg model  │──┐
 └──────────────┘            │   (on device)    │  │ symptom
                             └──────────────────┘  ▼
 ┌──────────────┐  readings  ┌──────────────────┐  ┌───────────────┐
 │ ESP32 sensor │──────────▶│ Threshold engine │─▶│  Rule engine  │──▶ condition +
 │    device    │ AP/BT/LAN  │  LOW/NORMAL/HIGH │  └───────────────┘    recommendations
 └──────────────┘            └──────────────────┘
                                                      │
                          SQLDelight (local) ◀────────┴────▶ Supabase (cloud sync)
```

### Healthy ranges used for chili

| Reading          | Range              |
|------------------|--------------------|
| Soil moisture    | 40 – 70 %          |
| Soil temperature | 20 – 30 °C         |
| Soil pH          | 5.5 – 7.0          |
| Soil EC          | 200 – 800 µS/cm    |
| Nitrogen         | 20 – 40 mg/kg      |
| Phosphorus       | 10 – 30 mg/kg      |
| Potassium        | 100 – 250 mg/kg    |
| Light            | 10,000 – 50,000 lux|
| Air temperature  | 18 – 35 °C         |

These live in `domain/engine/SiliGuideThresholds.kt`.

---

## Tech stack

| Area            | Tools                                                          |
|-----------------|----------------------------------------------------------------|
| Language / UI   | Kotlin, Jetpack Compose, Material 3, Navigation Compose        |
| Architecture    | Model-View-ViewModel MVVM, Hilt (KSP)                          |
| Local data      | SQLDelight, DataStore                                          |
| Background work | WorkManager                                                    |
| Camera / ML     | CameraX, LiteRT (TFLite), OpenCV, ML Kit barcode scanning      |
| Networking      | Ktor (OkHttp), Bluetooth Classic SPP                           |
| Cloud           | Supabase (Auth, Postgrest, Storage, Edge Functions)            |
| Sign-in         | Credential Manager (Google ID)                                 |
| Security        | AES-256-GCM sensor payloads, Android Keystore for stored tokens |

- **minSdk** 30 (Android 11), **targetSdk** 36
- Java / JVM target 11

---

## Project structure

```
app/src/main/java/com/example/siliguide/
├── core/            # ML (LeafSegmenter, ContourExtractor, DetectionRenderer),
│                    # security, permissions, image utils, DI modules, theme
├── data/
│   ├── cache/       # In-memory analysis cache
│   ├── local/       # SQLDelight mappers, DataStore preferences, session
│   ├── remote/
│   │   ├── iot/     # Esp32Api (HTTP), BluetoothApi, DTOs, AES-GCM crypto
│   │   └── supabase/
│   ├── repository/  # Repository implementations
│   └── sync/        # SyncEngine + SyncWorker
├── domain/
│   ├── engine/      # SiliGuideThresholds, ThresholdEngine, RuleEngine
│   ├── model/
│   ├── repository/  # Repository interfaces
│   └── usecase/     # Single-purpose use cases
└── presentation/
    ├── navigation/  # NavGraph
    └── features/    # analysis, auth, dashboard, iot, plants

app/src/main/sqldelight/   # Local database schema (SiliGuide.sq)
app/src/main/assets/models # TFLite models
supabase/functions/        # Edge functions (delete-account)
```

---

## Getting started

### Requirements

- Android Studio (latest stable)
- JDK 11 or newer
- An Android phone running Android 11+ (camera and Bluetooth are needed for the full app)
- A Supabase project (only for sign-in and cloud sync)

### 1. Clone

```bash
git clone https://github.com/itsCell27/siliguide.git
cd siliguide
```

### 2. Add your keys

Create or edit `local.properties` in the project root. This file is git-ignored, so never commit it.

```properties
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_PUBLISHABLE_KEY=your-publishable-key
GOOGLE_WEB_CLIENT_ID=your-google-web-client-id.apps.googleusercontent.com
```

Leave these blank if you only want to try guest mode.

### 3. Build and run

Open the project in Android Studio and press **Run**, or build from the command line:

```bash
./gradlew assembleDebug
```

### 4. Run the tests

```bash
./gradlew testDebugUnitTest
```

The rule engine is covered by unit tests in `app/src/test`.

---

## IoT device

SiliGuide reads soil and air data from an ESP32 sensor device. The app connects to it over Hotspot (AP), Bluetooth or LAN.

For the hardware, wiring, schematics and firmware, see the IoT device repository:
**[Phylakes01/Captone-esp-32-Scematics-and-IOT-code](https://github.com/Phylakes01/Captone-esp-32-Scematics-and-IOT-code)**

---

## Security

- Each device has its own 64-hex pairing token. Once a device is claimed, it stops showing the token, so another phone can't claim it.
- After pairing, sensor data travels as AES-256-GCM ciphertext and the app decrypts it locally.
- The app stores the pairing token encrypted with an Android Keystore key.
- Secret keys are kept in `local.properties`, never in source code.

---
