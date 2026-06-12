import { useState, useEffect, useCallback } from 'react';
import { websocketService } from './websocketService';
import { IChatMessage, MessageType } from './types';
import { useWebSocket } from '../../contexts/WebSocketContext';

export interface ChatHookProps {
  chatroomId?: number;
  userEmail?: string;
  productId?: number;
  useGlobalConnection?: boolean; // 전역 웹소켓 연결 사용 여부
  token?: string; // 독립 연결시 사용할 토큰
}

export interface ChatHookReturn {
  messages: IChatMessage[];
  sendMessage: (productId: number, content: string, messageType?: MessageType) => void;
  sendImage: (imageUrl: string) => void;
  isConnected: boolean;
  connect: () => void;
  disconnect: () => void;
}

/**
 * 채팅 기능을 위한 React Hook
 * @param props 채팅에 필요한 설정
 * @returns 채팅 관련 상태와 기능
 */
export const useChat = ({ 
  chatroomId, 
  productId,
  userEmail, 
  useGlobalConnection = true, 
  token 
}: ChatHookProps): ChatHookReturn => {
  const [messages, setMessages] = useState<IChatMessage[]>([]);
  const [localConnected, setLocalConnected] = useState<boolean>(false);
  const [isConnecting, setIsConnecting] = useState<boolean>(false);
  const globalWebSocket = useWebSocket();
  console.log("useChat productId",productId);
  // 전역 연결 또는 로컬 연결 상태 확인
  const isConnected = useGlobalConnection 
    ? globalWebSocket.isConnected
    : localConnected;

  // 웹소켓 연결
  const connect = useCallback(async () => {
    if (!userEmail || !token) {
      console.error('연결에 필요한 정보가 없습니다.');
      return;
    }

    if (isConnected || isConnecting) {
      console.log('이미 연결되어 있거나 연결 시도 중입니다.');
      return;
    }

    try {
      setIsConnecting(true);
      if (useGlobalConnection) {
        globalWebSocket.connect(token);
      } else {
        websocketService.connect(token);
        // 연결 상태 확인을 위해 잠시 대기
        await new Promise(resolve => setTimeout(resolve, 1000));
        const connected = websocketService.isConnected();
        setLocalConnected(connected);
        console.log('웹소켓 연결 상태:', connected);
      }
    } catch (error) {
      console.error('웹소켓 연결 오류:', error);
      setLocalConnected(false);
    } finally {
      setIsConnecting(false);
    }
  }, [userEmail, token, useGlobalConnection, globalWebSocket, isConnected, isConnecting]);

  // 웹소켓 연결 해제
  const disconnect = useCallback(() => {
    if (useGlobalConnection) {
      // 전역 웹소켓은 필요할 때만 연결 해제
    } else {
      websocketService.disconnect();
      setLocalConnected(false);
    }
  }, [useGlobalConnection]);

  // 컴포넌트 마운트 시 한 번만 연결 시도
  useEffect(() => {
    if (!useGlobalConnection && userEmail && token && !isConnected && !isConnecting) {
      connect();
    }

    return () => {
      if (!useGlobalConnection) {
        disconnect();
      }
    };
  }, []);  // 빈 의존성 배열로 마운트 시에만 실행

  // 연결 상태 모니터링 (5초마다)
  useEffect(() => {
    if (!useGlobalConnection) {
      const checkConnection = () => {
        const connected = websocketService.isConnected();
        if (localConnected !== connected) {
          setLocalConnected(connected);
        }
      };

      const intervalId = setInterval(checkConnection, 5000);
      return () => clearInterval(intervalId);
    }
  }, [useGlobalConnection, localConnected]);

  // 채팅방 구독
  useEffect(() => {
    if (!chatroomId || !userEmail || !isConnected) return;

    const subscriptionId = websocketService.subscribeToChatRoom(chatroomId, (message) => {
      console.log('새 메시지 수신:', message);
      setMessages((prevMessages) => {
        const isDuplicate = prevMessages.some(
          (m) => m.messageId === message.messageId
        );
        if (isDuplicate) {
          console.log('중복 메시지 무시:', message);
          return prevMessages;
        }
        console.log('새 메시지 추가:', message);
        return [...prevMessages, message];
      });
    });

    return () => {
      if (subscriptionId) {
        websocketService.unsubscribe(subscriptionId);
      }
    };
  }, [chatroomId, productId, userEmail, isConnected]);

  // 메시지 전송
  const sendMessage = useCallback(
    (productId: number, content: string, messageType: MessageType = MessageType.TEXT) => {
      if (!chatroomId || !userEmail || !isConnected) {
        console.error('메시지를 보낼 수 없습니다. 연결 상태와 필수 정보를 확인하세요.');
        return;
      }

      try {
        // 서버로 메시지 전송만 하고 로컬 상태 업데이트는 제거
        // 웹소켓 구독을 통해 메시지를 받으면 자동으로 상태가 업데이트됨
        websocketService.sendChatMessage({
          chatroomId,
          productId,
          content,
          messageType,
          senderEmail: userEmail
        });
      } catch (error) {
        console.error('메시지 전송 오류:', error);
      }
    },
    [chatroomId, userEmail, isConnected, productId]
  );

  // 이미지 전송
  const sendImage = useCallback(
    (imageUrl: string) => {
      if (!productId) {
        console.error('상품 ID가 없습니다.');
        return;
      }
      sendMessage(productId, imageUrl, MessageType.IMAGE);
    },
    [sendMessage, productId]
  );

  return {
    messages,
    sendMessage,
    sendImage,
    isConnected,
    connect,
    disconnect,
  };
}; 