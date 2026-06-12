import { useState, useEffect, useCallback } from 'react';
import { websocketService } from './websocketService';
import { INotification } from './types';

export interface NotificationHookProps {
  userEmail?: string;
  token?: string;
  currentChatroomId?: number;
}

export interface NotificationHookReturn {
  notifications: INotification[];
  isConnected: boolean;
  connect: () => void;
  disconnect: () => void;
  clearNotifications: () => void;
}

/**
 * 실시간 알림 기능을 위한 React Hook
 * @param props 알림 설정에 필요한 속성들
 * @returns 알림 관련 상태와 기능
 */
export const useNotification = ({
  userEmail,
  token
}: NotificationHookProps): NotificationHookReturn => {
  const [notifications, setNotifications] = useState<INotification[]>([]);
  const [isConnected, setIsConnected] = useState<boolean>(false);
  const [lastMessageId, setLastMessageId] = useState<string>('');

  // 웹소켓 연결
  const connect = useCallback(() => {
    if (!userEmail) {
      console.error('사용자 이메일이 제공되지 않았습니다.');
      return;
    }

    try {
      console.log(`[알림] ${userEmail} 사용자의 웹소켓 연결 시도...`);
      websocketService.connect(token);
      
      // 연결 상태를 주기적으로 확인하는 간단한 방법
      setTimeout(() => {
        const connected = websocketService.isConnected();
        console.log(`[알림] 웹소켓 연결 상태: ${connected}`);
        setIsConnected(connected);
      }, 1000);
    } catch (error) {
      console.error('웹소켓 연결 오류:', error);
    }
  }, [userEmail, token]);

  // 웹소켓 연결 해제
  const disconnect = useCallback(() => {
    websocketService.disconnect();
    setIsConnected(false);
  }, []);

  // 알림 초기화
  const clearNotifications = useCallback(() => {
    setNotifications([]);
  }, []);

  // 사용자 알림 구독
  useEffect(() => {
    if (!userEmail || !websocketService.isConnected()) {
      console.log(`[알림] 구독 조건 미충족: userEmail=${!!userEmail}, connected=${websocketService.isConnected()}`);
      return;
    }

    console.log(`[알림] ${userEmail} 사용자의 알림 구독 시작...`);
    const subscriptionId = websocketService.subscribeToUserNotifications(
      userEmail,
      (notification) => {
        console.log('[알림] 새 알림 수신:', notification);

        // 고유 메시지 ID 생성
        const messageId = `${notification.receiverEmail}-${notification.message}-${notification.timestamp || new Date().toISOString()}`;
        
        // 마지막으로 받은 메시지와 동일한지 확인하여 중복 방지
        if (messageId === lastMessageId) {
          console.log('[알림] 중복 알림 무시:', messageId);
          return;
        }
        
        // 새 알림을 추가하고 마지막 메시지 ID 업데이트
        setLastMessageId(messageId);
        setNotifications((prevNotifications) => [
          ...prevNotifications,
          {
            ...notification,
            timestamp: notification.timestamp || new Date().toISOString()
          }
        ]);
      }
    );

    return () => {
      if (subscriptionId) {
        console.log(`[알림] ${subscriptionId} 구독 해제`);
        websocketService.unsubscribe(subscriptionId);
      }
    };
  }, [userEmail, isConnected, lastMessageId]);

  // 웹소켓 연결 상태 모니터링
  useEffect(() => {
    const checkConnectionStatus = () => {
      const connected = websocketService.isConnected();
      if (isConnected !== connected) {
        console.log(`[알림] 연결 상태 변경: ${isConnected} -> ${connected}`);
        setIsConnected(connected);
      }
    };

    const intervalId = setInterval(checkConnectionStatus, 5000);
    return () => clearInterval(intervalId);
  }, [isConnected]);

  // 컴포넌트 마운트 시 한 번 연결
  useEffect(() => {
    if (userEmail && token) {
      connect();
    }

    return () => {
      disconnect();
    };
  }, [connect, disconnect, userEmail, token]);

  return {
    notifications,
    isConnected,
    connect,
    disconnect,
    clearNotifications
  };
}; 