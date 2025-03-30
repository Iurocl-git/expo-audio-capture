export interface FftData {
  low: number;
  mid: number;
  high: number;
}

export type NormalizationMode = 'default' | 'log' | 'adaptive';

export interface ExpoAudioCaptureModule {
  startCapture(): void;
  stopCapture(): void;
  setUdpConfig(ip: string, port: number): void;
  setNormalizationMode(mode: NormalizationMode): void;
  addListener(eventName: 'onFftData', listener: (data: FftData) => void): void;
  removeListener(eventName: 'onFftData', listener: (data: FftData) => void): void;
}

declare const module: ExpoAudioCaptureModule;
export default module; 