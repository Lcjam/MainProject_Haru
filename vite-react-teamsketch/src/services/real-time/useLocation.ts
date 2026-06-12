import { useState, useEffect, useCallback } from 'react';
import { websocketService } from './websocketService';
import { ILocation } from './types';  // types.ts에서 ILocation 타입을 가져옵니다.

export interface LocationHookProps {
  chatroomId?: number;
  userEmail?: string;
  token?: string;
}

export interface LocationHookReturn {
  locations: Map<string, ILocation>;
  sendLocation: (location: Omit<ILocation, 'email'>) => void;
  isConnected: boolean;
  connect: () => void;
  disconnect: () => void;
}

/**
 * 실시간 위치 공유 기능을 위한 React Hook
 * @param props 위치 공유에 필요한 설정
 * @returns 위치 관련 상태와 기능
 */
export const useLocation = ({
  chatroomId,
  userEmail,
  token
}: LocationHookProps): LocationHookReturn => {
  const [locations, setLocations] = useState<Map<string, ILocation>>(new Map());
  const [isConnected, setIsConnected] = useState<boolean>(false);
  const [isConnecting, setIsConnecting] = useState<boolean>(false);

  // 웹소켓 연결
  const connect = useCallback(() => {
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
      websocketService.connect(token);
      
      // 연결 상태를 주기적으로 확인하는 간단한 방법
      setTimeout(() => {
        const connected = websocketService.isConnected();
        console.log(`[위치] 웹소켓 연결 상태: ${connected}`);
        setIsConnected(connected);
      }, 1000);
    } catch (error) {
      console.error('웹소켓 연결 오류:', error);
      setIsConnected(false);
    } finally {
      setIsConnecting(false);
    }
  }, [userEmail, token]);

  // 웹소켓 연결 해제
  const disconnect = useCallback(() => {
    websocketService.disconnect();
    setIsConnected(false);
  }, []);

  // 위치 정보 구독
  useEffect(() => {
    if (!chatroomId || !userEmail || !isConnected) {
      console.log(`[위치] 구독 조건 미충족: chatroomId=${chatroomId}, userEmail=${userEmail}, connected=${isConnected}`);
      return;
    }

    console.log(`[위치] ${chatroomId} 채팅방의 위치 정보 구독 시작...`);
    const subscriptionId = websocketService.subscribeToLocation(
      chatroomId,
      (location) => {  // 타입 어노테이션 제거
        console.log('[위치] 새 위치 정보 수신:', location);
        setLocations(prev => {
          const newLocations = new Map(prev);
          if (location.email) {  // email이 undefined일 수 있으므로 체크
            newLocations.set(location.email, location);
          }
          return newLocations;
        });
      }
    );

    return () => {
      if (subscriptionId) {
        console.log(`[위치] ${subscriptionId} 구독 해제`);
        websocketService.unsubscribe(subscriptionId);
      }
    };
  }, [chatroomId, userEmail, isConnected]);

  // 위치 정보 전송
  const sendLocation = useCallback(
    (location: Omit<ILocation, 'email'>) => {
      if (!chatroomId || !userEmail || !isConnected) {
        console.error('위치를 전송할 수 없습니다. 연결 상태와 필수 정보를 확인하세요.');
        return;
      }

      try {
        websocketService.sendLocation({
          ...location,
          email: userEmail,
          timestamp: new Date().toISOString()
        });
      } catch (error) {
        console.error('위치 전송 오류:', error);
      }
    },
    [chatroomId, userEmail, isConnected]
  );

  // 웹소켓 연결 상태 모니터링
  useEffect(() => {
    const checkConnectionStatus = () => {
      const connected = websocketService.isConnected();
      if (isConnected !== connected) {
        console.log(`[위치] 연결 상태 변경: ${isConnected} -> ${connected}`);
        setIsConnected(connected);
      }
    };

    const intervalId = setInterval(checkConnectionStatus, 5000);
    return () => clearInterval(intervalId);
  }, [isConnected]);

  // 컴포넌트 마운트 시 한 번 연결
  useEffect(() => {
    if (userEmail && token && !isConnected && !isConnecting) {
      connect();
    }

    return () => {
      disconnect();
    };
  }, []);

  return {
    locations,
    sendLocation,
    isConnected,
    connect,
    disconnect
  };
};
