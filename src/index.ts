import ExpoAudioCaptureModule from './ExpoAudioCaptureModule';
import type { ExpoAudioCaptureModule as ExpoAudioCaptureModuleType } from './ExpoAudioCaptureModule.types';

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
export interface FftData {
  low: number;
  mid: number;
  high: number;
}

// Добавляем подписку на события
export function addFftDataListener(listener: (data: FftData) => void): void {
  const module = ExpoAudioCaptureModule as unknown as ExpoAudioCaptureModuleType;
  module.addListener('onFftData', listener);
}

export function removeFftDataListener(listener: (data: FftData) => void): void {
  const module = ExpoAudioCaptureModule as unknown as ExpoAudioCaptureModuleType;
  module.removeListener('onFftData', listener);
}

// Экспортируем все функции как единый объект
export default {
  startCapture,
  stopCapture,
  setUdpConfig,
  addFftDataListener,
  removeFftDataListener,
};
