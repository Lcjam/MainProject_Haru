import { createSlice, PayloadAction } from '@reduxjs/toolkit';
import {IPasswordChange} from '../../types/passwordChange'

// 초기 상태값
const initialState: IPasswordChange = {
    isToken: '',  // token 값 (optional)
    email: '',
    phoneNumber: '',
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
};

// Redux Slice 생성
const passwordChangeSlice = createSlice({
  name: 'passwordChange',
  initialState,
  reducers: {
    updatePasswordInfo: (state, action: PayloadAction<Partial<IPasswordChange>>) => {
      Object.assign(state, action.payload); // Immer를 사용하여 안전하게 업데이트
    },
    resetPasswordInfo: () => initialState, // 비밀번호 변경 정보를 초기 상태로 리셋하는 액션 추가
  },
});

// 액션 및 리듀서 내보내기
export const { updatePasswordInfo, resetPasswordInfo } = passwordChangeSlice.actions;
export default passwordChangeSlice.reducer;
