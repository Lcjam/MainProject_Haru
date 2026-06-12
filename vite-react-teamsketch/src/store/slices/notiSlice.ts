import { createSlice, PayloadAction } from '@reduxjs/toolkit';

// 알림 타입 정의
export type NotificationType = 'CHAT' | 'PRODUCT_REQUEST' | 'JOIN_REQUEST' | 'LOCATION_SHARE' | 'SYSTEM' | 'PRODUCT_APPROVAL' | string;

// 알림 상태 정의
export type NotificationStatus = 'READ' | 'UNREAD';

// 알림 인터페이스 정의
export interface INotification {
  id: number;
  type: NotificationType;
  message: string;
  timestamp: string;
  status: NotificationStatus;
  receiverEmail: string;
  senderEmail?: string;
  productId?: number;
  chatroomId?: number;
}

// 알림 상태 인터페이스
interface NotificationState {
  notifications: INotification[];
  unreadCount: number;
  lastProcessedId: string;
  unreadChatCount: number;
}

// 초기 상태
const initialState: NotificationState = {
  notifications: [],
  unreadCount: 0,
  lastProcessedId: '',
  unreadChatCount: 0
};

// 알림 슬라이스 생성
const notificationSlice = createSlice({
  name: 'notifications',
  initialState,
  reducers: {
    // 새 알림 추가
    addNotification: (state, action: PayloadAction<INotification>) => {
      // 중복 체크
      const isDuplicate = state.notifications.some(
        n => n.id === action.payload.id
      );
      
      if (!isDuplicate) {
        state.notifications.unshift(action.payload);
        state.unreadCount += 1;
      }
    },
    
    // 알림 읽음 처리
    markAsRead: (state, action: PayloadAction<number>) => {
      const notification = state.notifications.find(n => n.id === action.payload);
      if (notification && notification.status === 'UNREAD') {
        notification.status = 'READ';
        state.unreadCount = Math.max(0, state.unreadCount - 1);
      }
    },
    
    // 모든 알림 읽음 처리
    markAllAsRead: (state) => {
      state.notifications.forEach(n => {
        n.status = 'READ';
      });
      state.unreadCount = 0;
    },
    
    // 특정 채팅방의 모든 메시지 읽음 처리
    markChatMessagesAsRead: (state, action: PayloadAction<number>) => {
      const chatroomId = action.payload;
      let readCount = 0;
      
      state.notifications.forEach(n => {
        if (n.type === 'CHAT_MESSAGE' && n.chatroomId === chatroomId && n.status === 'UNREAD') {
          n.status = 'READ';
          readCount++;
        }
      });
      
      state.unreadCount = Math.max(0, state.unreadCount - readCount);
      state.unreadChatCount = Math.max(0, state.unreadChatCount - readCount);
    },
    
    // 알림 삭제
    removeNotification: (state, action: PayloadAction<number>) => {
      const notification = state.notifications.find(n => n.id === action.payload);
      if (notification && notification.status === 'UNREAD') {
        state.unreadCount = Math.max(0, state.unreadCount - 1);
      }
      state.notifications = state.notifications.filter(n => n.id !== action.payload);
    },
    
    // 마지막 처리된 알림 ID 업데이트
    updateLastProcessedId: (state, action: PayloadAction<string>) => {
      state.lastProcessedId = action.payload;
    },
    
    // 알림 초기화
    clearNotifications: (state) => {
      state.notifications = [];
      state.unreadCount = 0;
      state.lastProcessedId = '';
      state.unreadChatCount = 0;
    },
    
    // 안읽은 채팅 메시지 수 업데이트
    updateUnreadChatCount: (state, action: PayloadAction<number>) => {
      state.unreadChatCount = action.payload;
    },
    
    // 채팅 메시지를 제외한 모든 알림 읽음 처리
    markAllNonChatAsRead: (state) => {
      state.notifications.forEach(n => {
        if (n.type !== 'CHAT_MESSAGE' && n.status === 'UNREAD') {
          n.status = 'READ';
        }
      });
      // 채팅 메시지를 제외한 읽지 않은 알림 수 계산
      state.unreadCount = state.notifications.filter(
        n => n.status === 'UNREAD' && n.type === 'CHAT_MESSAGE'
      ).length;
    },
  }
});

// 액션 생성자 내보내기
export const {
  addNotification,
  markAsRead,
  markAllAsRead,
  markChatMessagesAsRead,
  removeNotification,
  updateLastProcessedId,
  clearNotifications,
  updateUnreadChatCount,
  markAllNonChatAsRead,
} = notificationSlice.actions;

// 리듀서 내보내기
export default notificationSlice.reducer;
