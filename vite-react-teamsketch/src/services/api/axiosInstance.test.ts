import { describe, it, expect, beforeEach } from 'vitest';
import type { InternalAxiosRequestConfig } from 'axios';
import { axiosInstance } from './axiosInstance';

// 인터셉터 함수는 외부로 export되지 않으므로 axios가 등록해 둔 handlers 배열에서 꺼내 직접 호출한다.
// (axios 내부 구조이지만 테스트에서 인터셉터 단위 검증에 널리 쓰이는 안정적 접근)
type Handler<A extends unknown[], R> = { fulfilled: (...args: A) => R; rejected: (e: unknown) => unknown };

const requestHandler = (axiosInstance.interceptors.request as unknown as {
  handlers: Handler<[InternalAxiosRequestConfig], InternalAxiosRequestConfig>[];
}).handlers[0];

const responseHandler = (axiosInstance.interceptors.response as unknown as {
  handlers: Handler<[{ data: unknown }], unknown>[];
}).handlers[0];

const makeConfig = (): InternalAxiosRequestConfig =>
  ({ headers: {} } as unknown as InternalAxiosRequestConfig);

describe('axiosInstance request interceptor', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('토큰이 있으면 Authorization 헤더에 Bearer로 주입', () => {
    localStorage.setItem('token', 'jwt-abc');
    const config = requestHandler.fulfilled(makeConfig());
    expect(config.headers.Authorization).toBe('Bearer jwt-abc');
  });

  it('토큰이 없으면 Authorization 헤더를 설정하지 않음', () => {
    const config = requestHandler.fulfilled(makeConfig());
    expect(config.headers.Authorization).toBeUndefined();
  });
});

describe('axiosInstance response interceptor', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('정상 응답은 그대로 통과', () => {
    const response = { data: { status: 'success', data: { id: 1 } } };
    expect(responseHandler.fulfilled(response)).toBe(response);
  });

  it("body.status가 'error'면 reject", async () => {
    const response = { data: { status: 'error', message: '실패' } };
    await expect(responseHandler.fulfilled(response) as Promise<unknown>).rejects.toThrow('실패');
  });

  it('401 + AUTH_EXPIRED면 메시지 문구와 관계없이 계정 상태를 지운다', async () => {
    localStorage.setItem('token', 'jwt-abc');
    const error = {
      config: { url: '/core/user' },
      response: { status: 401, data: { code: 'AUTH_EXPIRED', message: 'Session expired' } }
    };
    await expect(responseHandler.rejected(error) as Promise<unknown>).rejects.toBe(error);
    expect(localStorage.getItem('token')).toBeNull();
  });

  it('401 + AUTH_REQUIRED는 다른 계정 상태를 지우지 않는다', async () => {
    localStorage.setItem('token', 'jwt-abc');
    const error = { config: { url: '/core/user' }, response: {
      status: 401, data: { code: 'AUTH_REQUIRED', message: '만료라는 문구와 관계없음' }
    } };
    await expect(responseHandler.rejected(error) as Promise<unknown>).rejects.toBe(error);
    expect(localStorage.getItem('token')).toBe('jwt-abc');
  });

  it('401 + AUTH_INVALID는 계정 상태를 지운다', async () => {
    localStorage.setItem('token', 'jwt-abc');
    const error = { config: { url: '/core/user' }, response: {
      status: 401, data: { code: 'AUTH_INVALID', message: 'Bad credential' }
    } };
    await expect(responseHandler.rejected(error) as Promise<unknown>).rejects.toBe(error);
    expect(localStorage.getItem('token')).toBeNull();
  });
});
