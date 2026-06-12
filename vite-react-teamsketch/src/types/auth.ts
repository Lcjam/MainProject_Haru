export type LoginMethod = 'EMAIL' | 'SOCIAL';
export type SocialProvider = 'GOOGLE' | 'KAKAO' | 'NAVER' | null | 'NONE';

export interface ProfileUpdateRequest {
  name: string;
  nickname: string;
  bio: string;
  hobbies: HobbiesRequest[];
}

export interface HobbiesRequest{
  hobbyId: number;
  categoryId: number;
}
export interface SignupForm {
  name: string;
  password: string;
  email: string;
  phoneNumber: string;
  bio?: string;
  nickname: string;
  hobbies: HobbiesRequest[];
  profileImage?: File;
  loginMethod: LoginMethod;
  socialProvider: SocialProvider;
}
// 백엔드 ENUM과 매핑하기 위한 상수 추가
export const SOCIAL_PROVIDER = {
  GOOGLE: 'GOOGLE',
  KAKAO: 'KAKAO',
  NAVER: 'NAVER',
  NONE: 'NONE'
} as const;

export const LOGIN_METHOD = {
  EMAIL: 'EMAIL',
  SOCIAL: 'SOCIAL'
} as const;

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  status: 'success' | 'error';
  data: {
    id?: number;
    email?: string;
    name?: string;
    nickname?: string;
    phoneNumber?: string | null;
    bio?: string | null;
    loginMethod?: 'EMAIL' | 'SOCIAL';
    socialProvider?: 'GOOGLE' | 'KAKAO' | 'NAVER' | 'NONE';
    accountStatus?: 'Active' | 'Deactivated' | 'Dormant' | 'Withdrawal';
    authority?: 'USER' | 'ADMIN';
    profileImagePath?: string | null;
    token?: string;
  };
  message?: string;
  code: string;
}

