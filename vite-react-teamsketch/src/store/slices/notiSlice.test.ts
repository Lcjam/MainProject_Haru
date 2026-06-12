import { describe, it, expect } from 'vitest';
import notiReducer, {
  addNotification,
  markAsRead,
  markAllAsRead,
  removeNotification,
  clearNotifications,
  updateUnreadChatCount,
  updateLastProcessedId,
  type INotification
} from './notiSlice';

const initialState = {
  notifications: [] as INotification[],
  unreadCount: 0,
  lastProcessedId: '',
  unreadChatCount: 0
};

const makeNoti = (id: number, over: Partial<INotification> = {}): INotification => ({
  id,
  type: 'SYSTEM',
  message: `msg-${id}`,
  timestamp: '2026-01-01T00:00:00Z',
  status: 'UNREAD',
  receiverEmail: 'me@haru.com',
  ...over
});

describe('notiSlice', () => {
  it('addNotification: 맨 앞에 추가하고 unreadCount 증가', () => {
    let state = notiReducer(initialState, addNotification(makeNoti(1)));
    state = notiReducer(state, addNotification(makeNoti(2)));

    expect(state.notifications.map(n => n.id)).toEqual([2, 1]); // unshift
    expect(state.unreadCount).toBe(2);
  });

  it('addNotification: 동일 id는 중복 추가하지 않음', () => {
    let state = notiReducer(initialState, addNotification(makeNoti(1)));
    state = notiReducer(state, addNotification(makeNoti(1)));

    expect(state.notifications).toHaveLength(1);
    expect(state.unreadCount).toBe(1);
  });

  it('markAsRead: 해당 알림을 READ로 바꾸고 unreadCount 감소', () => {
    let state = notiReducer(initialState, addNotification(makeNoti(1)));
    state = notiReducer(state, markAsRead(1));

    expect(state.notifications[0].status).toBe('READ');
    expect(state.unreadCount).toBe(0);
  });

  it('markAsRead: 이미 READ면 unreadCount 변동 없음', () => {
    let state = notiReducer(initialState, addNotification(makeNoti(1)));
    state = notiReducer(state, markAsRead(1));
    state = notiReducer(state, markAsRead(1));
    expect(state.unreadCount).toBe(0);
  });

  it('markAllAsRead: 모두 READ + unreadCount 0', () => {
    let state = notiReducer(initialState, addNotification(makeNoti(1)));
    state = notiReducer(state, addNotification(makeNoti(2)));
    state = notiReducer(state, markAllAsRead());

    expect(state.notifications.every(n => n.status === 'READ')).toBe(true);
    expect(state.unreadCount).toBe(0);
  });

  it('removeNotification: UNREAD 삭제 시 unreadCount 감소', () => {
    let state = notiReducer(initialState, addNotification(makeNoti(1)));
    state = notiReducer(state, removeNotification(1));

    expect(state.notifications).toHaveLength(0);
    expect(state.unreadCount).toBe(0);
  });

  it('clearNotifications: 전체 초기화', () => {
    let state = notiReducer(initialState, addNotification(makeNoti(1)));
    state = notiReducer(state, updateLastProcessedId('abc'));
    state = notiReducer(state, clearNotifications());

    expect(state).toEqual(initialState);
  });

  it('updateUnreadChatCount: 값 설정', () => {
    const state = notiReducer(initialState, updateUnreadChatCount(5));
    expect(state.unreadChatCount).toBe(5);
  });
});
