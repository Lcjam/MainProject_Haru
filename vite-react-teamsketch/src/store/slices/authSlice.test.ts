import { describe, it, expect, beforeEach } from 'vitest';
import authReducer, { login, logout, setError } from './authSlice';

const initialState = {
  isAuthenticated: false,
  user: { email: null, nickname: null, userId: null },
  token: null,
  error: null
};

describe('authSlice', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('login: 상태를 채우고 token/user를 localStorage에 저장', () => {
    const payload = { email: 'a@haru.com', nickname: '하루', userId: 7, token: 'jwt-abc' };
    const state = authReducer(initialState, login(payload));

    expect(state.isAuthenticated).toBe(true);
    expect(state.token).toBe('jwt-abc');
    expect(state.user).toEqual({ email: 'a@haru.com', nickname: '하루', userId: 7 });
    expect(state.error).toBeNull();
    expect(localStorage.getItem('token')).toBe('jwt-abc');
    expect(JSON.parse(localStorage.getItem('user')!)).toEqual({
      email: 'a@haru.com',
      nickname: '하루',
      userId: 7
    });
  });

  it('logout: 상태 초기화 + 인증 관련 localStorage 키 제거', () => {
    localStorage.setItem('token', 'jwt-abc');
    localStorage.setItem('user', '{}');
    localStorage.setItem('persist:root', '{}');
    localStorage.setItem('locationSet', 'true');

    const loggedIn = {
      isAuthenticated: true,
      user: { email: 'a@haru.com', nickname: '하루', userId: 7 },
      token: 'jwt-abc',
      error: null
    };
    const state = authReducer(loggedIn, logout());

    expect(state.isAuthenticated).toBe(false);
    expect(state.token).toBeNull();
    expect(state.user).toEqual({ email: null, nickname: null, userId: null });
    expect(localStorage.getItem('token')).toBeNull();
    expect(localStorage.getItem('user')).toBeNull();
    expect(localStorage.getItem('persist:root')).toBeNull();
    expect(localStorage.getItem('locationSet')).toBeNull();
  });

  it('setError: error 메시지 설정', () => {
    const state = authReducer(initialState, setError('로그인 실패'));
    expect(state.error).toBe('로그인 실패');
  });
});
