// 상품 등록 요청 타입
export interface IProductRegisterRequest {
  title: string;
  description: string;
  price: number;
  categoryId: number;
  hobbyId: number;
  maxParticipants: number;
  transactionType: '대면' | '비대면';
  registrationType: '구매' | '판매';
  startDate: string;
  endDate: string;
  latitude?: number;
  longitude?: number;
  meetingPlace?: string;
  address?: string;
  days: string[];
  images?: File[];
}

// 상품 응답 타입
export interface IProduct {
  id: number;
  productCode: string;
  title: string;
  description: string;
  price: number;
  email: string;
  categoryId: number;
  hobbyId: number;
  transactionType: '대면' | '비대면';
  registrationType: '구매' | '판매';
  maxParticipants: number;
  currentParticipants: number;
  days: string[];
  startDate: string;
  endDate: string;
  latitude: number | null;
  longitude: number | null;
  meetingPlace: string | null;
  address: string | null;
  createdAt: string | null;
  imagePaths: string[];
  thumbnailPath: string | null;
  nickname: string | null;
  bio: string | null;
  dopamine: number;
  visible: boolean;
}

// API 응답 타입
export interface IApiResponse<T> {
  status: 'success' | 'error';
  message: string;
  data: T;
}

// 상품 등록 응답 타입
export type IProductRegisterResponse = IApiResponse<IProduct>;

// 상품 목록 조회 응답 타입
export type IProductListResponse = IApiResponse<IProduct[]>;
