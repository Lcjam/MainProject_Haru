import React, { createContext, useContext, useState, useEffect } from 'react';
import { websocketService } from '../services/real-time/websocketService';

interface WebSocketContextType {
  isConnected: boolean;
  connect: (token?: string) => void;
  disconnect: () => void;
}

const WebSocketContext = createContext<WebSocketContextType | undefined>(undefined);

interface WebSocketProviderProps {
  children: React.ReactNode;
  token?: string;
  autoConnect?: boolean;
}

export const WebSocketProvider: React.FC<WebSocketProviderProps> = ({
  children,
  token,
  autoConnect = true
}) => {
  const [isConnected, setIsConnected] = useState<boolean>(false);

  // 웹소켓 연결
  const connect = (newToken?: string) => {
    try {
      const finalToken = newToken || token;
      websocketService.connect(finalToken);
      setIsConnected(websocketService.isConnected());
    } catch (error) {
      console.error('웹소켓 연결 오류:', error);
    }
  };

  // 웹소켓 연결 해제
  const disconnect = () => {
    websocketService.disconnect();
    setIsConnected(false);
  };

  // 자동 연결 (autoConnect가 true이고 토큰이 있을 때)
  useEffect(() => {
    if (autoConnect && token) {
      connect();
    }

    return () => {
      disconnect();
    };
  }, [autoConnect, token]);

  const value = {
    isConnected,
    connect,
    disconnect
  };

  return (
    <WebSocketContext.Provider value={value}>
      {children}
    </WebSocketContext.Provider>
  );
};

// 커스텀 훅으로 컨텍스트 접근을 쉽게 함
export const useWebSocket = (): WebSocketContextType => {
  const context = useContext(WebSocketContext);
  if (context === undefined) {
    throw new Error('useWebSocket은 WebSocketProvider 내에서 사용되어야 합니다');
  }
  return context;
}; 