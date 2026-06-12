import { createSlice, PayloadAction } from '@reduxjs/toolkit';

interface Category {
  categoryId: number;
  categoryName: string;
}

interface Hobby {
  hobbyId: number;
  hobbyName: string;
  categories: Array<{
    categoryId: number;
    categoryName: string;
  }>;
}

interface CategoryState {
  // 변경 가능한 카테고리/취미 상태 (선택용)
  categories: Category[];
  hobbies: Hobby[];
  selectedCategoryId: number | null;
  loading: boolean;
  error: string | null;

  // 상수형 카테고리/취미 상태 (표시용)
  constantCategories: Category[];
  constantHobbies: Hobby[];
}

const initialState: CategoryState = {
  categories: [],
  hobbies: [],
  selectedCategoryId: null,
  loading: false,
  error: null,
  constantCategories: [],
  constantHobbies: []
};

const categorySlice = createSlice({
  name: 'category',
  initialState,
  reducers: {
    setCategories: (state, action: PayloadAction<Category[]>) => {
      state.categories = action.payload;
    },
    setHobbies: (state, action: PayloadAction<Hobby[]>) => {
      state.hobbies = action.payload;
    },
    setSelectedCategoryId: (state, action: PayloadAction<number | null>) => {
      state.selectedCategoryId = action.payload;
    },
    setLoading: (state, action: PayloadAction<boolean>) => {
      state.loading = action.payload;
    },
    setError: (state, action: PayloadAction<string | null>) => {
      state.error = action.payload;
    },
    // 상수형 카테고리/취미 설정 액션 추가
    setConstantCategories: (state, action: PayloadAction<Category[]>) => {
      state.constantCategories = action.payload;
    },
    setConstantHobbies: (state, action: PayloadAction<Hobby[]>) => {
      state.constantHobbies = action.payload;
    }
  }
});

export const {
  setCategories,
  setHobbies,
  setSelectedCategoryId,
  setLoading,
  setError,
  setConstantCategories,
  setConstantHobbies,
} = categorySlice.actions;

export default categorySlice.reducer;
