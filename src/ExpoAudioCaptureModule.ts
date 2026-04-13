import { NativeModule, requireNativeModule } from 'expo';

declare class ExpoAudioCaptureModule extends NativeModule {
  startCapture(): void;
  stopCapture(): void;
  setUdpConfig(ip: string, port: number): void;
}

// This call loads the native module object from the JSI.
export default requireNativeModule<ExpoAudioCaptureModule>('ExpoAudioCapture');
