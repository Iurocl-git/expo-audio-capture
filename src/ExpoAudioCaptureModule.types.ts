export interface ExpoAudioCaptureModule {
  startCapture(): void;
  stopCapture(): void;
  setUdpConfig(ip: string, port: number): void;
  addListener(eventName: 'onFftData', listener: (data: { low: number; mid: number; high: number }) => void): void;
  removeListener(eventName: 'onFftData', listener: (data: { low: number; mid: number; high: number }) => void): void;
}

declare const module: ExpoAudioCaptureModule;
export default module; 