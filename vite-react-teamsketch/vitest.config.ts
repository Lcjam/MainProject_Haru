import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';
import path from 'path';

// 테스트 전용 설정. 프로덕션 vite.config.ts의 PWA/opencv 청크 설정을 끌어들이지 않아
// 테스트 기동이 가볍고 빠르다. alias('@')만 동일하게 맞춰 import 경로를 일치시킨다.
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src')
    }
  },
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    include: ['src/**/*.{test,spec}.{ts,tsx}'],
    css: false
  }
});
