import { Client, IMessage, StompSubscription, IFrame } from '@stomp/stompjs';
import { WebSocketConfig, IChatMessage, INotification, ILocation } from './types';
import { apiConfig } from '../api/apiConfig';

// 싱글톤 인스턴스
let stompClient: Client | null = null;
const subscriptions: Map<string, StompSubscription> = new Map();

// 환경 변수에서 백엔드 URL 가져오기 (기본값은 localhost:8080)
const BACKEND_URL = apiConfig.endpoints.core.websocket;
console.log("BACKEND_URL",BACKEND_URL);
/**
 * WebSocket 연결 설정 및 관리를 위한 서비스
 */
export const websocketService = {
  /**
   * WebSocket 연결 초기화 및 설정
   * @param token JWT 토큰
   * @param config WebSocket 추가 설정
   * @returns WebSocket 클라이언트 인스턴스
   */
  connect: (token?: string, config?: Partial<WebSocketConfig>): Client => {
    if (stompClient?.connected) {
      console.log('이미 WebSocket이 연결되어 있습니다.');
      return stompClient;
    }

    if (stompClient?.active) {
      console.log('WebSocket이 연결 중입니다.');
      return stompClient;
    }

    // 기존 클라이언트가 있다면 정리
    if (stompClient) {
      stompClient.deactivate();
      stompClient = null;
    }

    // 기본 설정
    const defaultConfig: WebSocketConfig = {
      brokerURL: BACKEND_URL,
      reconnectDelay: 5000,
      heartbeatIncoming: 20000,
      heartbeatOutgoing: 20000,
      debug: true // 디버깅 활성화
    };

    // 사용자 설정과 기본 설정 병합
    const finalConfig = { ...defaultConfig, ...config };

    // Stomp 클라이언트 생성
    stompClient = new Client({
      brokerURL: finalConfig.brokerURL,
      reconnectDelay: finalConfig.reconnectDelay,
      heartbeatIncoming: finalConfig.heartbeatIncoming,
      heartbeatOutgoing: finalConfig.heartbeatOutgoing,
      connectHeaders: token ? { 'Authorization': `Bearer ${token}` } : {},
      onConnect: () => {
        console.log('WebSocket 연결 성공');
        // 연결 시 구독 재구성
        websocketService.resubscribeAll();
      },
      onDisconnect: () => {
        console.log('WebSocket 연결 해제');
      },
      onStompError: (frame: IFrame) => {
        console.error('WebSocket 에러:', frame);
      },
      // debug 함수 설정 - 디버깅 강화
    //   debug: (str) => {
    //     console.log('[STOMP]', str);
    //   }
    });

    // 연결 시작
    stompClient.activate();
    return stompClient;
  },

  /**
   * WebSocket 연결 해제
   */
  disconnect: (): void => {
    if (stompClient && stompClient.connected) {
      stompClient.deactivate();
      stompClient = null;
      subscriptions.clear();
      console.log('WebSocket 연결 해제됨');
    }
  },

  /**
   * 특정 채팅방 메시지 구독
   * @param chatroomId 채팅방 ID
   * @param callback 메시지 수신 시 호출할 콜백 함수
   * @returns 구독 ID (구독 해제 시 필요)
   */
  subscribeToChatRoom: (chatroomId: number, callback: (message: IChatMessage) => void): string => {
    if (!stompClient || !stompClient.connected) {
      console.error('WebSocket이 연결되지 않았습니다. 연결 시도 중...');
      websocketService.connect();
      
      const destination = `/topic/room.${chatroomId}`;
      websocketService.saveSubscriptionCallback(destination, (message: IMessage) => {
        try {
          console.log('채팅 메시지 수신:', message.body); // 디버깅용 로그 추가
          const chatMessage: IChatMessage = JSON.parse(message.body);
          callback(chatMessage);
        } catch (error) {
          console.error('메시지 파싱 오류:', error);
        }
      });
      
      return destination;
    }

    const destination = `/topic/room.${chatroomId}`;
    
    if (subscriptions.has(destination)) {
      console.log(`이미 채팅방 ${chatroomId}를 구독 중입니다.`);
      return destination;
    }

    console.log(`채팅방 ${chatroomId} 구독 시작`);
    const subscription = stompClient.subscribe(destination, (message: IMessage) => {
      try {
        console.log('채팅 메시지 수신:', message.body); // 디버깅용 로그 추가
        const chatMessage: IChatMessage = JSON.parse(message.body);
        callback(chatMessage);
      } catch (error) {
        console.error('메시지 파싱 오류:', error);
      }
    });

    subscriptions.set(destination, subscription);
    return destination;
  },

  /**
   * 개인 알림 구독
   * @param userEmail 사용자 이메일
   * @param callback 알림 수신 시 호출할 콜백 함수
   * @returns 구독 ID (구독 해제 시 필요)
   */
  subscribeToUserNotifications: (userEmail: string, callback: (notification: INotification) => void): string => {
    if (!userEmail) {
      console.error('WebSocket 알림 구독 오류: 사용자 이메일이 누락되었습니다.');
      return '';
    }
    
    if (!stompClient || !stompClient.connected) {
      console.error('WebSocket이 연결되지 않았습니다. 연결 시도 중...');
      websocketService.connect();
      
      // 나중에 구독을 위해 콜백 함수를 저장
      const destination = `/topic/user/${userEmail}`;
      console.log(`웹소켓 연결 안됨: ${destination} 경로 콜백 저장`);
      websocketService.saveSubscriptionCallback(destination, (message: IMessage) => {
        try {
          console.log(`받은 알림 메시지(저장된 콜백): ${message.body}`);
          const notification: INotification = JSON.parse(message.body);
          callback(notification);
        } catch (error) {
          console.error('알림 메시지 파싱 오류:', error);
        }
      });
      
      return destination;
    }

    const destination = `/topic/user/${userEmail}`;
    
    // 이미 구독 중인지 확인
    if (subscriptions.has(destination)) {
      console.log(`이미 사용자 ${userEmail}의 알림을 구독 중입니다.`);
      return destination;
    }

    console.log(`사용자 ${userEmail}의 알림 구독 시작: ${destination}`);
    
    // 새 구독 생성
    try {
      const subscription = stompClient.subscribe(destination, (message: IMessage) => {
        console.log(`알림 메시지 수신(${destination}):`, message.body);
        try {
          const notification: INotification = JSON.parse(message.body);
          callback(notification);
        } catch (error) {
          console.error('알림 메시지 파싱 오류:', error);
          console.error('파싱 실패한 원본 메시지:', message.body);
        }
      });

      // 구독 정보 저장
      subscriptions.set(destination, subscription);
      console.log(`알림 구독 성공: ${destination}`);
      return destination;
    } catch (error) {
      console.error(`알림 구독 중 오류(${destination}): ${error}`);
      return '';
    }
  },

  // 보류 중인 구독을 위한 콜백 함수 저장소
  pendingSubscriptions: new Map<string, (message: IMessage) => void>(),

  // 콜백 함수 저장
  saveSubscriptionCallback: (destination: string, callback: (message: IMessage) => void): void => {
    websocketService.pendingSubscriptions.set(destination, callback);
  },

  // 연결 후 모든 대기 중인 구독 복원
  resubscribeAll: (): void => {
    if (!stompClient || !stompClient.connected) return;

    websocketService.pendingSubscriptions.forEach((callback, destination) => {
      console.log(`재구독 시도: ${destination}`);
      const subscription = stompClient!.subscribe(destination, callback);
      subscriptions.set(destination, subscription);
    });

    // 처리된 대기 구독 정리
    websocketService.pendingSubscriptions.clear();
  },

  /**
   * 메시지 전송
   * @param destination 목적지 (예: /app/chat.message)
   * @param body 전송할 메시지 본문
   */
  send: (destination: string, body: unknown): void => {
    if (!stompClient || !stompClient.connected) {
      console.error('WebSocket이 연결되지 않았습니다.');
      return;
    }

    stompClient.publish({
      destination,
      body: JSON.stringify(body),
      headers: { 'content-type': 'application/json' }
    });
  },

  /**
   * 채팅 메시지 전송
   * @param message 전송할 채팅 메시지
   */
  sendChatMessage: (message: Omit<IChatMessage, 'messageId' | 'sentAt' | 'senderName' | 'senderProfileUrl'>): void => {
    const messageToSend = {
      chatroomId: message.chatroomId,
      productId: message.productId,
      content: message.content,
      messageType: message.messageType,
      senderEmail: message.senderEmail
    };
    websocketService.send('/app/chat/send', messageToSend);
  },

  /**
   * 구독 해제
   * @param subscriptionId 구독 ID
   */
  unsubscribe: (subscriptionId: string): void => {
    const subscription = subscriptions.get(subscriptionId);
    if (subscription) {
      subscription.unsubscribe();
      subscriptions.delete(subscriptionId);
      console.log(`구독 해제: ${subscriptionId}`);
    }
  },

  /**
   * 모든 구독 해제
   */
  unsubscribeAll: (): void => {
    subscriptions.forEach((subscription, id) => {
      subscription.unsubscribe();
      console.log(`구독 해제: ${id}`);
    });
    subscriptions.clear();
  },

  /**
   * 연결 상태 확인
   * @returns 연결 상태 (true: 연결됨, false: 연결 안됨)
   */
  isConnected: (): boolean => {
    return !!(stompClient && stompClient.connected);
  },

  /**
   * 위치 정보 구독
   * @param chatroomId 채팅방 ID
   * @param callback 위치 정보 수신 시 호출될 콜백 함수
   */
  subscribeToLocation: (chatroomId: number, callback: (location: ILocation) => void): string => {
    const destination = `/topic/location.${chatroomId}`;
    
    if (!stompClient || !stompClient.connected) {
      console.log(`[위치] WebSocket 연결 안됨. 구독 대기 목록에 추가: ${destination}`);
      websocketService.pendingSubscriptions.set(destination, (message) => {
        const locationData = JSON.parse(message.body);
        callback(locationData);
      });
      return destination;
    }

    console.log(`[위치] 구독 시작: ${destination}`);
    const subscription = stompClient.subscribe(destination, (message) => {
      const locationData = JSON.parse(message.body);
      callback(locationData);
    });

    subscriptions.set(destination, subscription);
    return destination;
  },

  /**
   * 위치 정보 전송
   * @param location 전송할 위치 정보
   */
  sendLocation: (location: ILocation): void => {
    if (!stompClient || !stompClient.connected) {
      console.error('WebSocket이 연결되지 않았습니다.');
      return;
    }

    websocketService.send(`/app/location/${location.chatroomId}`, location);
  }
}; 