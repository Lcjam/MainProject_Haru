export interface IMessage {
  role: 'user' | 'assistant';
  content: string;
}

export interface IChatState {
  messages: IMessage[];
  isLoading: boolean;
  error: string | null;
}
