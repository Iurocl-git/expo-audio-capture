# expo-audio-capture

Native Expo module for capturing system audio on Android using MediaProjection, performing FFT analysis, and sending data via UDP.

> ⚠️ iOS is not supported because screen/system audio capture is not allowed.

---

## 📦 Installation

```bash
npm install expo-audio-capture
npx expo run:android
```

> Requires `expo-dev-client` or bare workflow

---

## 🚀 Usage Example

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

## 📚 API Reference

### `startCapture()`
Prompts the user to allow screen/audio capture using MediaProjection.

### `stopCapture()`
Stops MediaProjection and the audio recording.

### `setUdpConfig(ip: string, port: number)`
Sets the IP and port to which FFT data will be sent over UDP.

### `addFftDataListener(callback)`
Subscribes to FFT data events with values like `{ low, mid, high }`.

### `removeFftDataListener(callback)`
Unsubscribes a previously registered listener.

---

## 🔐 Permissions

Permissions are automatically declared by the module:
- `RECORD_AUDIO`
- `FOREGROUND_SERVICE_MEDIA_PROJECTION`

However, you **must manually request runtime permissions** in your app using `PermissionsAndroid` or `expo-permissions`:

```ts
import { PermissionsAndroid } from 'react-native';

await PermissionsAndroid.request(PermissionsAndroid.PERMISSIONS.RECORD_AUDIO);
```

> Without this, capture may silently fail.

---

## ⚙️ Customization

This module is written in Kotlin and fully open-source. You can customize it depending on your needs:

### 🔌 Disable UDP
- Simply do not call `setUdpConfig()`.
- Or remove `sendUdpData(...)` from the Kotlin source.

### 📊 Raw FFT Data
- By default, emitted FFT data is normalized to [0–255].
- Modify `startRecording()` if you want raw magnitudes.

### 🛠 Suggested Enhancements
You may want to implement:
- `setNormalizeFFT(enabled: boolean)`
- `enableUdp(enabled: boolean)`

To make your module more configurable.

---

## 📖 Documentation Links

- [Latest stable API docs](https://docs.expo.dev/versions/latest/sdk/audio-capture/)
- [Main branch docs](https://docs.expo.dev/versions/unversioned/sdk/audio-capture/)

---

## 🙌 Contributing

PRs and issues are welcome: [github.com/Iurocl-git/expo-audio-capture](https://github.com/Iurocl-git/expo-audio-capture)

---

## 🪪 License

MIT

