# CS 501 Individual Assignment 6 Question 3 — Sound Meter

## Explanation

The **Sound Meter** app measures real-time audio levels using the device’s microphone.
It uses `AudioRecord` to read sound amplitude, converts it into an approximate **decibel (dB)** value, and displays a visual noise meter.
The meter changes color from green → yellow → red depending on loudness, and shows a warning if the sound exceeds a threshold.

The UI is built with **Jetpack Compose** and updates automatically as new microphone data arrives.

## How to Use

1. Run the app on a **real Android device** (the emulator does not support microphone input).
2. Allow the app to access the **microphone** when prompted.
3. Make noise near the phone — speak, clap, or play music.
4. Watch the dB number and meter bar update in real time:
   * Quiet = green
   * Moderate = yellow
   * Loud = red + a warning message
5. Stay quiet to watch the level drop back down.

## Implementation

* **Audio Input**: `AudioRecord` reads raw PCM audio samples.
* **Decibel Conversion**: Uses a simple formula `dB = 20 \log_{10}(maxAmp / 32767) + 90` to produce readable values.
* **State Management**: A `mutableStateOf<Float>` stores the current dB and triggers UI updates.
* **Visual Meter**: A horizontal bar that fills and changes color based on sound intensity.
* **Permission Handling**: Includes the `RECORD_AUDIO` permission and checks it at runtime.
