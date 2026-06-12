import { createSlice } from '@reduxjs/toolkit';

// DB 구조와 일치하는 타입 정의
export interface IUser {
  id: number;
  email: string;
  name: string;
  phoneNumber: string | null;
  nickname: string;
  bio: string | null;
  dopamine: number;
  points: number;
  loginMethod: 'EMAIL' | 'SOCIAL';
  socialProvider: 'GOOGLE' | 'KAKAO' | 'NAVER' | 'NONE';
  accountStatus: 'Active' | 'Deactivated' | 'Dormant' | 'Withdrawal';
  authority: 'USER' | 'ADMIN';
  signupDate: string;
  lastUpdateDate: string;
  lastLoginTime: string | null;
  loginFailedAttempts: number;
  loginIsLocked: boolean;
  profileImagePath: File | null;
  interest: string[];
  hobby: Array<{
    hobbyId: number;
    hobbyName: string;
    categoryId: number;
    categoryName: string;
  }>;
}

interface UserState {
  user: IUser;
}

const initialState: UserState = {
  user: {
    id: 0,
    email: '',
    name: '',
    phoneNumber: null,
    nickname: '',
    bio: null,
    dopamine: 0,
    points: 0,
    loginMethod: 'EMAIL',
    socialProvider: 'NONE',
    accountStatus: 'Active',
    authority: 'USER',
    signupDate: '',
    lastUpdateDate: '',
    lastLoginTime: null,
    loginFailedAttempts: 0,
    loginIsLocked: false,
    profileImagePath: null,
    interest: [],
    hobby: []
  }
};

const userSlice = createSlice({
  name: 'user',
  initialState,
  reducers: {
    setUser: (state, action) => {
      state.user = {
        ...state.user,
        ...action.payload
      };
    },
    clearUser: (state) => {
      state.user = initialState.user;
    },
    updateProfileImage: (state, action) => {
      state.user.profileImagePath = action.payload;
      localStorage.setItem('user', JSON.stringify(state.user));
    }
  }
});

export const { setUser, clearUser, updateProfileImage } = userSlice.actions;
export default userSlice.reducer;
