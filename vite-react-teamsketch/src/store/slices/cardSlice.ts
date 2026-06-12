import { createSlice, PayloadAction } from '@reduxjs/toolkit';

// 카드 정보 타입 정의
export interface ICardInfo {
  cardNum_1: number;
  cardNum_2: number;
  cardNum_3: number;
  cardNum_4: number;
  cardName: string;
  cardExpiry: string;
  lastName: string;
  firstName: string;
}

// 초기 상태값
const initialState: ICardInfo = {
  cardNum_1: 0,
  cardNum_2: 0,
  cardNum_3: 0,
  cardNum_4: 0,
  cardName: '',
  cardExpiry: '',
  lastName: '',
  firstName: ''
};

// Redux Slice 생성
const cardInfoSlice = createSlice({
  name: 'cardInfo',
  initialState,
  reducers: {
    updateCardInfo: (state, action: PayloadAction<Partial<ICardInfo>>) => {
      Object.assign(state, action.payload); // Immer를 사용하여 안전하게 업데이트
    },
    resetCardInfo: () => initialState // 카드 정보를 초기 상태로 리셋하는 액션 추가
  }
});

// 액션 및 리듀서 내보내기
export const { updateCardInfo, resetCardInfo } = cardInfoSlice.actions;
export default cardInfoSlice.reducer;
