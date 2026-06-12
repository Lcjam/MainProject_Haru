import '@testing-library/jest-dom';
import { afterEach } from 'vitest';
import { cleanup } from '@testing-library/react';

// 각 테스트 후 DOM/마운트 정리 + localStorage 격리
afterEach(() => {
  cleanup();
  localStorage.clear();
});
