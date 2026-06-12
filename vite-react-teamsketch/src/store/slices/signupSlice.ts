import { createSlice, PayloadAction } from '@reduxjs/toolkit';
import { SignupForm, HobbiesRequest } from '../../types/auth';

interface ValidationErrors {
  name: string;
  password: string;
  email: string;
  phoneNumber: string;
  nickname: string;
  hobby: string;
}

interface SignupState {
  formData: SignupForm;
  validationErrors: ValidationErrors;
  error: string;
}

const initialState: SignupState = {
  formData: {
    name: '',
    password: '',
    email: '',
    phoneNumber: '',
    bio: '',
    nickname: '',
    hobbies: [],
    loginMethod: 'EMAIL',
    socialProvider: 'NONE'
  },
  validationErrors: {
    name: '',
    password: '',
    email: '',
    phoneNumber: '',
    nickname: '',
    hobby: ''
  },
  error: ''
};

const signupSlice = createSlice({
  name: 'signup',
  initialState,
  reducers: {
    updateField: (
      state,
      action: PayloadAction<{ 
        name: keyof SignupForm; 
        value: string 
      }>
    ) => {
      (state.formData[action.payload.name] as any) = action.payload.value;
    },
    setValidationError: (state, action: PayloadAction<{ field: string; message: string }>) => {
      state.validationErrors[action.payload.field as keyof ValidationErrors] =
        action.payload.message;
    },
    setError: (state, action: PayloadAction<string>) => {
      state.error = action.payload;
    },
    resetSignupInfo: () => initialState,
    addHobby: (state, action: PayloadAction<HobbiesRequest>) => {
      const exists = state.formData.hobbies?.some(
        hobby => hobby.hobbyId === action.payload.hobbyId && 
                hobby.categoryId === action.payload.categoryId
      );
      
      if (!exists) {
        state.formData.hobbies?.push(action.payload);
      }
    },
    removeHobby: (state, action: PayloadAction<HobbiesRequest>) => {
      state.formData.hobbies = state.formData.hobbies?.filter(
        hobby => !(hobby.hobbyId === action.payload.hobbyId && 
                  hobby.categoryId === action.payload.categoryId)
      );
    }
  }
});

export const { 
  updateField, 
  setValidationError, 
  setError, 
  addHobby, 
  removeHobby,
  resetSignupInfo
} = signupSlice.actions;
export default signupSlice.reducer;
