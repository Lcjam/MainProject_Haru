import axios, { AxiosInstance } from 'axios';
import { apiConfig } from './apiConfig';
import { store } from '../../store/store';
import { logout } from '../../store/slices/authSlice';
// 1번 이미 있는 인스턴스 사용하면 됨됨
// API 응답 타입 정의
interface ApiResponse<T = any> {
  status: 'success' | 'error';
  data?: T;
  message?: string;
  code?: string;
}

// 에러 메시지 중복 방지를 위한 변수
let isErrorToastShown = false;
const errorToastTimeout = 5000; // 5초 후 다시 에러 메시지 표시 가능

// 이미지 관련 URL 패턴 (404 에러 메시지를 표시하지 않을 URL 패턴)
const imageUrlPatterns = [
  /\/images\//,
  /\/products\/images\//,
  /\/profiles\/image\//
];

// 기본 axios 인스턴스 생성
// 헤더에 콘텐츠타입이 앱 json 형태로 보낸다는것이고
// withCredentials: true 이게 브라우저가 가지고 있는 토큰을 함께 보내는 겁니다
// 이 인스턴스(객체)는 기본url과 타임아웃, 헤더설정, 토큰전송여부를 가지고 있는 인스턴스입니다다
export const axiosInstance = axios.create({
  baseURL: apiConfig.baseURL,
  timeout: 180000,
  headers: {
    'Content-Type': 'application/json'
  },
  withCredentials: true
});

// 파일 업로드용 인스턴스
// 얘의 경우는 콘텐츠 타입이 위와 다르게 멀티파트로 파일용입니다다
export const uploadInstance = axios.create({
  baseURL: apiConfig.baseURL,
  timeout: 30000,
  headers: {
    'Content-Type': 'multipart/form-data'
  },
  withCredentials: true
});

// URL이 이미지 관련 패턴과 일치하는지 확인하는 함수
const isImageRequest = (url: string): boolean => {
  return imageUrlPatterns.some(pattern => pattern.test(url));
};

// 에러 토스트 메시지 표시 함수 (중복 방지)
const showErrorToast = (message: string) => {
  if (!isErrorToastShown) {
    isErrorToastShown = true;
    console.log(message);
    
    // 일정 시간 후 다시 에러 메시지를 표시할 수 있도록 설정
    setTimeout(() => {
      isErrorToastShown = false;
    }, errorToastTimeout);
  }
};

// 공통 인터셉터 설정
const setupInterceptors = (instance: AxiosInstance) => {
  instance.interceptors.request.use(
    (config) => {
      const token = localStorage.getItem('token');
      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
      }
      return config;
    },
    (error) => {
      console.error('Request Error:', error);
      return Promise.reject(error);
    }
  );

  instance.interceptors.response.use(
    (response) => {
      const apiResponse = response.data as ApiResponse;
      if (apiResponse.status === 'error') {
        return Promise.reject(new Error(apiResponse.message));
      }
      return response;
    },
    (error) => {
      if (error.response) {
        const url = error.config?.url || '';
        
        switch (error.response.status) {
          case 401:
            // 토큰이 완전히 만료된 경우에만 로그아웃 처리
            if (error.response.data?.message?.includes('만료')) {
              store.dispatch(logout());
              localStorage.removeItem('token');
              showErrorToast('인증이 만료되었습니다. 다시 로그인해주세요.');
            } else {
              // 다른 401 에러의 경우 메시지만 표시
              console.log(error.response.data?.message || '인증 오류가 발생했습니다.');
            }
            break;
          case 403:
            console.log('접근 권한이 없습니다.');
            break;
          case 404:
            // 이미지 관련 요청의 404 에러는 콘솔에만 로그하고 토스트 메시지는 표시하지 않음
            if (isImageRequest(url)) {
              console.log(`이미지 로드 실패 (404): ${url}`);
            } else {
              console.log('요청하신 리소스를 찾을 수 없습니다.');
            }
            break;
          case 429:
            console.log('요청이 너무 많습니다. 잠시 후 다시 시도해주세요.');
            break;
          case 500:
            console.log('서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.');
            break;
          default:
            console.log('오류가 발생했습니다.');
        }
      } else if (error.request) {
        console.log('서버에 연결할 수 없습니다. 네트워크 연결을 확인해주세요.');
      }
      return Promise.reject(error);
    }
  );
};

// 일반 인터셉터 적용
setupInterceptors(axiosInstance);
setupInterceptors(uploadInstance);

