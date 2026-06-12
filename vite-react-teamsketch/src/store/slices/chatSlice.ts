import { createSlice, PayloadAction } from '@reduxjs/toolkit';

interface Message {
  role: 'user' | 'assistant';
  content: string;
}

interface LastMessage {
  messageId: number;
  chatroomId: number;
  senderEmail: string;
  content: string;
  messageType: string;
  sentAt: string;
  read: boolean;
  senderName: string;
  senderProfileUrl: string;
  productId: number;
}

interface ChatState {
  messages: Message[];
  lastMessage: LastMessage[];
  isLoading: boolean;
  error: string | null;
}

const initialState: ChatState = {
  messages: [],
  lastMessage: [],
  isLoading: false,
  error: null
};

const chatSlice = createSlice({
  name: 'chat',
  initialState,
  reducers: {
    sendMessage: (state, action: PayloadAction<string>) => {
      state.messages.push({
        role: 'user',
        content: action.payload
      });
      state.isLoading = true;
      state.error = null;
    },
    receiveMessage: (state, action: PayloadAction<string>) => {
      state.messages.push({
        role: 'assistant',
        content: action.payload
      });
      state.isLoading = false;
    },
    setError: (state, action: PayloadAction<string>) => {
      state.error = action.payload;
      state.isLoading = false;
    },
    setLastMessage: (state, action: PayloadAction<LastMessage>) => {
      state.lastMessage.push(action.payload);
    }
  }
});

export const { sendMessage, receiveMessage, setError } = chatSlice.actions;
export default chatSlice.reducer;
