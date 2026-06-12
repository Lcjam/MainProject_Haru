import { createSlice, PayloadAction } from '@reduxjs/toolkit';
import { IProduct, IProductRegisterRequest } from '../../types/product';

interface ProductState {
  products: IProduct[];
  selectedProduct: IProduct | null;
  loading: boolean;
  isLoading: boolean;
  error: string | null;
  distance: number;
  formData: Partial<IProductRegisterRequest>;
  registerForm: {
    title: string;
    description: string;
    price: number | null;
    categoryId: number;
    hobbyId: number;
    transactionType: '대면' | '비대면';
    registrationType: '판매' | '구매';
    maxParticipants: number;
    currentParticipants: number;
    meetingPlace: string;
    address: string;
    latitude: number | null;
    longitude: number | null;
    images: File[];
    days: string[];
    startDate: string;
    endDate: string;
  };
  selectedCategory: number | null;
}


const initialState: ProductState = {
  products: [],
  selectedProduct: null,
  loading: false,
  isLoading: false,
  error: null,
  distance: 10,
  formData: {},
  registerForm: {
    title: '',
    description: '',
    price: null,
    categoryId: 0,
    hobbyId: 0,
    transactionType: '대면',
    registrationType: '판매',
    maxParticipants: 1,
    currentParticipants: 0,
    meetingPlace: '',
    address: '',
    latitude: null,
    longitude: null,
    images: [],
    days: [],
    startDate: '',
    endDate: ''
  },
  selectedCategory: null
};

const productSlice = createSlice({
  name: 'product',
  initialState,
  reducers: {
    // 상품 목록 설정
    setProducts: (state, action: PayloadAction<IProduct[]>) => {
      state.products = action.payload;
      state.error = null;
    },
    // 선택된 상품 설정
    setSelectedProduct: (state, action: PayloadAction<IProduct | null>) => {
      state.selectedProduct = action.payload;
      state.error = null;
    },
    // 로딩 상태 설정
    setLoading: (state, action: PayloadAction<boolean>) => {
      state.loading = action.payload;
    },
    // 에러 상태 설정
    setError: (state, action: PayloadAction<string | null>) => {
      state.error = action.payload;
    },
    // 폼 데이터 업데이트
    updateProductForm: (state, action: PayloadAction<Partial<IProductRegisterRequest>>) => {
      state.registerForm = {
        ...state.registerForm,
        ...action.payload
      };
    },
    // 폼 데이터 초기화
    resetProductForm: (state) => {
      state.registerForm = {
        title: '',
        description: '',
        price: null,
        categoryId: 0,
        hobbyId: 0,
        transactionType: '대면',
        registrationType: '판매',
        maxParticipants: 1,
        currentParticipants: 0,
        meetingPlace: '',
        address: '',
        latitude: null,
        longitude: null,
        images: [],
        days: [],
        startDate: '',
        endDate: ''
      };
    },
    // 상품 등록 성공 처리
    productRegisterSuccess: (state, action: PayloadAction<IProduct>) => {
      state.products.push(action.payload);
      state.registerForm = {
        title: '',
        description: '',
        price: null,
        categoryId: 0,
        hobbyId: 0,
        transactionType: '대면',
        registrationType: '판매',
        maxParticipants: 1,
        currentParticipants: 0,
        meetingPlace: '',
        address: '',
        latitude: null,
        longitude: null,
        images: [],
        days: [],
        startDate: '',
        endDate: ''
      };
      state.error = null;
    },
    // 상품 등록 실패 처리
    productRegisterFailure: (state, action: PayloadAction<string>) => {
      state.error = action.payload;
    },
    // 이미지 추가
    addProductImage: (state, action: PayloadAction<File>) => {
      state.registerForm.images.push(action.payload);
    },
    // 이미지 제거
    removeProductImage: (state, action: PayloadAction<number>) => {
      state.registerForm.images.splice(action.payload, 1);
    },
    setMaxParticipants: (state, action: PayloadAction<number>) => {
      state.registerForm.maxParticipants = action.payload;
    },
    setCurrentParticipants: (state, action: PayloadAction<number>) => {
      state.registerForm.currentParticipants = action.payload;
    },
    setMeetingPlace: (state, action: PayloadAction<string>) => {
      state.registerForm.meetingPlace = action.payload;
    },
    setAddress: (state, action: PayloadAction<string>) => {
      state.registerForm.address = action.payload;
    },
    setLocation: (state, action: PayloadAction<{ latitude: number; longitude: number }>) => {
      state.registerForm.latitude = action.payload.latitude;
      state.registerForm.longitude = action.payload.longitude;
    },
    setDistance: (state, action: PayloadAction<number>) => {
      state.distance = action.payload;
    },
    resetProductState: () => {
      return initialState;
    },
    setSelectedCategory: (state, action: PayloadAction<number | null>) => {
      state.selectedCategory = action.payload;
    }
  }
});

export const {
  setProducts,
  setSelectedProduct,
  setLoading,
  setError,
  updateProductForm,
  resetProductForm,
  productRegisterSuccess,
  productRegisterFailure,
  addProductImage,
  removeProductImage,
  setMaxParticipants,
  setCurrentParticipants,
  setMeetingPlace,
  setAddress,
  setLocation,
  resetProductState,
  setDistance,
  setSelectedCategory
} = productSlice.actions;

export default productSlice.reducer;
