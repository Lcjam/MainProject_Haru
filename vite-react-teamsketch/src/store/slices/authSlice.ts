import { createSlice, PayloadAction } from '@reduxjs/toolkit';

// 사용자 정보 인터페이스 분리
interface User {
  email: string | null;
  nickname: string | null;
  userId: number | null;
}

// 인증 상태 인터페이스
interface AuthState {
  isAuthenticated: boolean;
  user: User;
  token: string | null;
  error: string | null;
}

// localStorage에서 초기 상태 가져오기
const savedUser = localStorage.getItem('user');
const parsedUser = savedUser ? JSON.parse(savedUser) : null;

// 초기 상태 정의
const initialState: AuthState = {
  isAuthenticated: !!localStorage.getItem('token'),
  user: parsedUser || {
    email: null,
    nickname: null,
    userId: null
  },
  token: localStorage.getItem('token'),
  error: null
};

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    login: (
      state,
      action: PayloadAction<{
        email: string;
        nickname: string;
        userId: number;
        token: string;
      }>
    ) => {
      state.isAuthenticated = true;
      state.user = {
        email: action.payload.email,
        nickname: action.payload.nickname,
        userId: action.payload.userId
      };
      state.token = action.payload.token;
      state.error = null;

      // 토큰을 localStorage에 저장
      localStorage.setItem('token', action.payload.token);
      // 사용자 기본 정보도 localStorage에 저장
      localStorage.setItem(
        'user',
        JSON.stringify({
          email: action.payload.email,
          nickname: action.payload.nickname,
          userId: action.payload.userId
        })
      );
    },
    logout: (state) => {
      state.isAuthenticated = false;
      state.user = {
        email: null,
        nickname: null,
        userId: null
      };
      state.token = null;
      state.error = null;

      // localStorage에서 인증 관련 정보 제거
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      localStorage.removeItem('persist:root');
      localStorage.removeItem('locationSet');
    },
    setError: (state, action: PayloadAction<string>) => {
      state.error = action.payload;
    }
  }
});

export const { login, logout, setError } = authSlice.actions;
export default authSlice.reducer;
