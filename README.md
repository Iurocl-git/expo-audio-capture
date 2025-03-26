# expo-audio-capture

🎧 Native Expo module for capturing system audio on Android using `MediaProjection`, performing FFT analysis, and sending data via UDP.

> ❗ iOS is not supported because screen/system audio capture is not allowed.

## Features

- ✅ Captures system audio using `MediaProjection`
- ✅ Performs real-time FFT analysis
- ✅ Sends data via UDP
- 🔊 Emits `onFftData` events with `{ low, mid, high }` values
- ❌ iOS: not supported (module will warn)

---

## Installation

```bash
npm install expo-audio-capture
```

Then rebuild the app:

```bash
npx expo run:android
```

> You must use a development build or eject, as this module includes native code.

### For bare React Native projects

Ensure you have [installed and configured the `expo` package](https://docs.expo.dev/bare/installing-expo-modules/) before using this module.

---

## Usage

```ts
import AudioCapture from 'expo-audio-capture';

AudioCapture.setUdpConfig('192.168.0.123', 8888);

AudioCapture.startCapture();

AudioCapture.addFftDataListener((data) => {
  console.log('FFT Data:', data);
});

// Later
AudioCapture.stopCapture();
```

---

## API

### `startCapture()`

Prompts the user to allow screen/audio capture.

### `stopCapture()`

Stops MediaProjection and recording.

### `setUdpConfig(ip: string, port: number)`

Sets the destination IP and port for sending FFT data over UDP.

### `addFftDataListener(callback)`

Subscribes to FFT data updates.

### `removeFftDataListener(callback)`

Unsubscribes a specific listener.

---

## Permissions

This module declares the necessary permissions automatically in the Android manifest, including:

- `RECORD_AUDIO`
- `FOREGROUND_SERVICE_MEDIA_PROJECTION` (Android 13+)

However, you **still need to request permissions at runtime**. You can use `PermissionsAndroid` or `expo-permissions` to prompt the user.

> Without runtime permission, audio capture may silently fail or crash.

---

## Limitations

- Android 10+ (API 29+) only
- No support for iOS
- Requires `expo-dev-client` or bare workflow

---

## Customization

This module is written in Kotlin and fully open-source. You can customize it depending on your needs:

### ✨ Use events only (without UDP)
If you don't want to send FFT data over the network, simply avoid calling `setUdpConfig(...)`. The module will still emit FFT data via the `onFftData` event.

You can also comment or remove the `sendUdpData(...)` calls inside the native code to fully disable UDP.

### 🎵 Raw vs Normalized Data
By default, the module emits **normalized** FFT values between `0` and `255`. If you prefer to work with raw values, you can modify the `startRecording()` method in `ExpoAudioCaptureModule.kt` and send the `magnitudes` array (or raw `audioBuffer`) directly through `sendEvent("onFftData", ...)`.

---

## Should this be included in the core code?
Yes, and it is recommended. You can consider adding configuration options such as:

- `setNormalizeFFT(enabled: boolean)` — to switch between normalized and raw FFT data.
- `enableUdp(enabled: boolean)` — to control whether data is sent over UDP.

These changes would make the module more flexible and allow more developers to use it for different use cases.

---

## API Documentation

- [Documentation for the latest stable release](https://docs.expo.dev/versions/latest/sdk/audio-capture/)
- [Documentation for the main branch](https://docs.expo.dev/versions/unversioned/sdk/audio-capture/)

> If no documentation appears, the module may not be supported in the latest managed Expo release.

---

## Contributing

Contributions are very welcome! Please refer to the guidelines described in the [Expo contributing guide](https://github.com/expo/expo#contributing).

---

## License

MIT

