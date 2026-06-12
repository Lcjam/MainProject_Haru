import { useMutation } from '@tanstack/react-query';
import { axiosInstance } from './axiosInstance';
import { apiConfig } from './apiConfig';
import { SignupForm, ProfileUpdateRequest } from '../../types/auth';
import { IPasswordChange } from '../../types/passwordChange'


interface LoginCredentials {
  email: string;
  password: string;
}

interface SignupResponse {
  success: boolean;
  message: string;
  data?: {
    userId: string;
    email: string;
  };
}

interface VerifyOtpRequest {
  phoneNumber: string;
  otp: string;
}

interface SmsResponse {
  statusCode: string;
  message: string;
}

interface VerifyOtpResponse {
  success: boolean;
  message: string;
}

interface PasswordChangeResponse {
  success: boolean;
  message: string;
}

interface PasswordResponse{
  token: string,
  currentPassword: string,
  newPassword: string,
  confirmPassword: string
}

// 로그인 API
const loginApi = async (credentials: LoginCredentials) => {
  const response = await axiosInstance.post(apiConfig.endpoints.core.login, credentials);
  console.log('로그인 응답:', response.data);
  return response.data;
};

// 회원가입 API
const signupApi = async (userData: SignupForm) => {
  const response = await axiosInstance.post<SignupResponse>(
    apiConfig.endpoints.core.signup,
    userData
  );
  return response.data;
};

// 로그아웃 API
const logoutApi = async () => {
  const response = await axiosInstance.post(apiConfig.endpoints.core.logout);
  return response.data;
};

// 유저정보조회 API
const getUserInfoApi = async () => {
  const response = await axiosInstance.get(apiConfig.endpoints.core.userinfo);
  return response.data;
};

// 문자 전송 API
const sendSmsApi = async (phoneNumber: string): Promise<SmsResponse> => {
  const response = await axiosInstance.post(apiConfig.endpoints.assist.sendSms, { phoneNumber });
  return response.data;
};

// 문자 인증 API
const verifyOtpApi = async ({ phoneNumber, otp }: VerifyOtpRequest): Promise<VerifyOtpResponse> => {
  const response = await axiosInstance.post(apiConfig.endpoints.assist.verifyOtp, {
    phoneNumber,
    otp
  });
  return response.data;
};

// 비토큰 비밀번호 변경
export const passwordChangeNoneToken = async (passwordRequestData: IPasswordChange): Promise<PasswordChangeResponse> => {
    const response = await axiosInstance.put<PasswordChangeResponse>(
      apiConfig.endpoints.core.passwordChangeNoneToken,
      passwordRequestData,
      {
        withCredentials: false,
      }
      
    );
    console.log("API response:", response);
    
    return response.data; // ✅ `response.data`를 반환
};

//회원 탈퇴
export interface IwithdrawRsponse{
  status: string,
  data: {
    success: boolean,
    message: string,
    withdrawalDate: Date,
  },
  code: string,
}
// 3번 요청 그리고 응답 구조 는 만들어서 사용해도 되고 그냥 해도 되고고
// 엔드포인트 제가 어제 user 컨트롤러 삭제하고 auth로 올겼던거 같은데 옮겼네요 그럼 엔드포인트가 auth/me/~
export const withdrawUserApi = async (password: string): Promise<IwithdrawRsponse> => {
  const response = await axiosInstance.post(apiConfig.endpoints.core.deleteUser,{ password });
  return response.data;
};
// 여기에 토큰 추가하는게 맞지만 저희가 지금 인스턴스 사용중인데,
// 미리 헤더를 포함하는 인스턴스를 만들어서 그걸 사용하기에
// 여기서는 따로 추가안해도 됩니다
// 요청기대값에 있는 isToken은 비밀번호 변경이
// 토큰이 있는 경우와 없는경우를 구분하기 위한 문자열비교를 통해서 분기를 만들어 주는 것입니다
export const passwordApi = async ({ isToken, currentPassword, newPassword, confirmPassword }: { isToken: string, currentPassword: string, newPassword: string, confirmPassword: string }):
  // 인스턴스에서 설정하지 않은 것을 이제 여기에 추가해서
  // 만드는데 아까 적은것들이 아닌 put, post 같은것을 명시해주고()안에 기본이 아닌 apiConfig에 설정한
  // 주소와 보낼 데이터를 담고 요청합니다 그러면 백엔드 주소인인 8080포트 gateway로 갑니다
  Promise<PasswordResponse> => {
  const response = await axiosInstance.put(apiConfig.endpoints.core.passwordChange, { isToken, currentPassword, newPassword, confirmPassword },
  );
  return response.data;
};

//사용자 위치 등록 mylocation
export interface LocationResponse {
  status: string;
  message: string;
  data: null;
}

export const saveLocationApi = async ({latitude, longitude, locationName}: {  latitude: number, longitude: number, locationName: string}): Promise<LocationResponse> => {
  const response = await axiosInstance.post(apiConfig.endpoints.core.mylocation, { latitude, longitude, locationName });
  return response.data;
};
  
//사용자가 등록한 목록 조회
interface Product {
  id: number;
  productCode: string;
  title: string;
  description: string;
  price: number;
  email: string;
  categoryId: number;
  hobbyId: number;
  transactionType: string;
  registrationType: string;
  maxParticipants: number;
  currentParticipants: number;
  days: string[];
  startDate: string;
  endDate: string;
  latitude: number | null;
  longitude: number | null;
  meetingPlace: string | null;
  address: string | null;
  createdAt: string;
  imagePaths: string[];
  thumbnailPath: string;
  nickname: string;
  bio: string;
  dopamine: number;
  visible: boolean;
}
interface ProductListResponse{
  status: string;
  message: string;
  data: Product[];
}
export type ProductType = "registers/buy" | "registers/sell" | "requests/buy" | "requests/sell";

export const myProdListApi = async (type: ProductType): Promise<ProductListResponse> => {
  const url = `/core/market/products/users/${type}`;
  const response = await axiosInstance.get(url);
  return response.data;
};

//사용자 거래 내역 조회
export interface Transactions{
  id: number;
  productId: number;
  buyerEmail: string;
  sellerEmail: string;
  transactionStatus: '진행중' | '완료';
  paymentStatus:  '미완료' | '완료';
  price: number;
  description: string;
  createdAt: number[];
}

export interface TransactionsResponse{
  status: 'success' | 'error';
  message: string;
  data: Transactions[] | null;
}

export const transactionsListApi = async (): Promise<TransactionsResponse> => {
  const response = await axiosInstance.get(apiConfig.endpoints.core.transactionslist);
  return response.data;
};

//사용자의 위치 기반 특정 반경 내의 상품 조회
export interface NProductResponse{
  status: string;
  message: string;
  data: {
    id: number; 
    name: string;
    price: number;
    imagePath: string[];
    thumbnailPath: string;
  }[];
}

export const nearbyProdListApi = async ({latitude, longitude, distance}: {  latitude: number, longitude: number, distance: number}): Promise<NProductResponse> => {
  const response = await axiosInstance.post(apiConfig.endpoints.core.nearbyprod, { latitude, longitude, distance });
  return response.data;
};
  


// 프로필 수정 API
const updateProfileApi = async (profileData: ProfileUpdateRequest) => {
  const response = await axiosInstance.put(apiConfig.endpoints.core.updateProfile, profileData);
  return response.data;
};

// 로그인 Hook
export const useLogin = () => {
  return useMutation({
    mutationFn: loginApi
  });
};

// 비토큰 비번 변경 훅
export const usePasswordChangeNT = () => {
  return useMutation({
    mutationFn: passwordChangeNoneToken
  });
};

// 회원가입 Hook
export const useSignup = () => {
  return useMutation({
    mutationFn: signupApi,
    onError: (error: any) => {
      // axiosInstance에서 기본 에러 처리를 하지만,
      // 회원가입 특화된 에러 처리가 필요한 경우 여기서 처리
      if (error.response?.data?.code === 'DUPLICATE_EMAIL') {
        throw new Error('이미 사용 중인 이메일입니다.');
      }
      if (error.response?.data?.code === 'DUPLICATE_ID') {
        throw new Error('이미 사용 중인 아이디입니다.');
      }
      throw error;
    }
  });
};

// 로그아웃 Hook
export const useLogout = () => {
  return useMutation({
    mutationFn: logoutApi
  });
};

// 유저정보조회 Hook
export const useInfoApi = () => {
  return useMutation({
    mutationFn: getUserInfoApi
  });
};

// 문자 전송 Hook
export const useSendSms = () => {
  return useMutation({
    mutationFn: sendSmsApi,
    onError: (error: any) => {
      console.error('SMS 전송 실패:', error);
      throw new Error(error.response?.data?.message || 'SMS 전송에 실패했습니다.');
    }
  });
};

// 문자 인증 Hook
export const useVerifyOtp = () => {
  return useMutation({
    mutationFn: verifyOtpApi,
    onError: (error: any) => {
      console.error('OTP 검증 실패:', error);
      throw new Error(error.response?.data?.message || '인증번호 확인에 실패했습니다.');
    }
  });
};

//회원탈퇴 Hook
export const useDeleteAccount = () => {
  return useMutation({
    mutationFn: withdrawUserApi// 회원 탈퇴 API 호출 함수
  });
}
//비밀번호 변경
export const useChangePassword = () => {
  return useMutation({
    mutationFn: passwordApi
  });
}
//사용자 등록,요청 상품 조회
export const useMyProdList = () => {
  return useMutation({
    mutationFn: myProdListApi
  });
}

//사용자 거래 내역 조회
export const useTrasactionsList = () => {
  return useMutation({
    mutationFn: transactionsListApi
  });
}

export const useNearbyProdList = () => {
  return useMutation({
    mutationFn: nearbyProdListApi
  });
}
// 프로필 수정 Hook
export const useUpdateProfile = () => {
  return useMutation({
    mutationFn: updateProfileApi
  });
};
