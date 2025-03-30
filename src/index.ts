import ExpoAudioCaptureModule from './ExpoAudioCaptureModule';
import type {
  ExpoAudioCaptureModule as ExpoAudioCaptureModuleType,
  FftData,
  NormalizationMode,
} from './ExpoAudioCaptureModule.types';

export type { FftData, NormalizationMode };

export function startCapture(): void {
  console.log('Starting audio capture');
  ExpoAudioCaptureModule.startCapture();
}

export function stopCapture(): void {
  console.log('Stopping audio capture');
  ExpoAudioCaptureModule.stopCapture();
}

export function setUdpConfig(ip: string, port: number): void {
  console.log(`Setting UDP config: IP = ${ip}, Port = ${port}`);
  ExpoAudioCaptureModule.setUdpConfig(ip, port);
}

// Добавляем типы для событий

// export type NormalizationMode = 'default' | 'log' | 'adaptive';

// Добавляем подписку на события
export function addFftDataListener(listener: (data: FftData) => void): void {
  const module = ExpoAudioCaptureModule as unknown as ExpoAudioCaptureModuleType;
  module.addListener('onFftData', listener);
}

export function removeFftDataListener(listener: (data: FftData) => void): void {
  const module = ExpoAudioCaptureModule as unknown as ExpoAudioCaptureModuleType;
  module.removeListener('onFftData', listener);
}

export function setNormalizationMode(mode: NormalizationMode): void {
  const module = ExpoAudioCaptureModule as unknown as ExpoAudioCaptureModuleType;
  console.log(`Setting normalization mode to: ${mode}`);
  module.setNormalizationMode(mode);
}
// Экспортируем все функции как единый объект
export default {
  startCapture,
  stopCapture,
  setUdpConfig,
  setNormalizationMode,
  addFftDataListener,
  removeFftDataListener,
};
