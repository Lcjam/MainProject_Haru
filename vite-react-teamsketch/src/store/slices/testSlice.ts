import { createSlice, PayloadAction } from '@reduxjs/toolkit';

interface TestState {
  value: number;
  count: number;
}

const initialState: TestState = {
  value: 1,
  count: 0
};

const testSlice = createSlice({
  name: 'test',
  initialState,
  reducers: {
    increment: (state, action: PayloadAction<number>) => {
      state.value *= action.payload;
      state.count += 1;
    },
    decrement: (state, action: PayloadAction<number>) => {
      state.value /= action.payload;
      state.count -= 1;
    }
  }
});

export const { increment, decrement } = testSlice.actions;
export default testSlice.reducer;
