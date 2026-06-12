import { useMutation } from '@tanstack/react-query';
import { axiosInstance } from './axiosInstance';
import { apiConfig } from './apiConfig';

interface IChatRequest {
  message: string;
  type: 'customer_service';
}

interface IChatResponse {
  status: string;
  data: {
    message: string;
    response: string;
  };
  message: string | null;
  code: string;
}

export const useSendChatMessage = () => {
  return useMutation({
    mutationFn: async (message: string) => {
      const request: IChatRequest = {
        message,
        type: 'customer_service'
      };

      const response = await axiosInstance.post<IChatResponse>(
        apiConfig.endpoints.assist.chat,
        request
      );

      if (response.data.status === 'error') {
        throw new Error(response.data.message || '채팅 요청 중 오류가 발생했습니다.');
      }

      return response.data.data.response;
    }
  });
};
