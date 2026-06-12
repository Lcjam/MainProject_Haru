import { createSlice, PayloadAction } from '@reduxjs/toolkit';

interface LoadingState {
  isLoading: boolean;
  loadingMessage?: string;
}

const initialState: LoadingState = {
  isLoading: false,
  loadingMessage: undefined
};

const loadingSlice = createSlice({
  name: 'loading',
  initialState,
  reducers: {
    startLoading: (state, action: PayloadAction<string | undefined>) => {
      state.isLoading = true;
      state.loadingMessage = action.payload;
    },
    stopLoading: (state) => {
      state.isLoading = false;
      state.loadingMessage = undefined;
    }
  }
});

export const { startLoading, stopLoading } = loadingSlice.actions;
export default loadingSlice.reducer;
