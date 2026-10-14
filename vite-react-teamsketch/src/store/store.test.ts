import { beforeEach, describe, expect, it, vi } from 'vitest';
import { queryClient, store } from './store';
import { login, logout } from './slices/authSlice';
import { setUser } from './slices/userSlice';
import { sendMessage } from './slices/chatSlice';
import { updateCardInfo } from './slices/cardSlice';
import { addNotification } from './slices/notiSlice';

describe('계정 데이터 격리', () => {
  const deleteCache = vi.fn().mockResolvedValue(true);

  beforeEach(() => {
    localStorage.clear();
    queryClient.clear();
    deleteCache.mockClear();
    vi.stubGlobal('caches', { delete: deleteCache });
  });

  it('A 로그아웃 뒤 B가 로그인해도 Redux, Query, API 캐시에 A 데이터가 남지 않는다', async () => {
    store.dispatch(login({ email: 'a@haru.com', nickname: 'A', userId: 1, token: 'token-a' }));
    store.dispatch(setUser({ email: 'a@haru.com', nickname: 'A' }));
    store.dispatch(sendMessage('A의 비공개 채팅'));
    store.dispatch(updateCardInfo({ cardName: 'A의 카드' }));
    store.dispatch(addNotification({
      id: 1,
      type: 'CHAT',
      message: 'A의 거래 알림',
      timestamp: '2026-09-29T00:00:00Z',
      status: 'UNREAD',
      receiverEmail: 'a@haru.com'
    }));
    queryClient.setQueryData(['profile'], { email: 'a@haru.com' });
    const transactions = queryClient.getMutationCache().build(queryClient, {
      mutationKey: ['transactions'],
      mutationFn: async () => [{ buyerEmail: 'a@haru.com', productId: 7 }]
    });
    await transactions.execute(undefined);
    deleteCache.mockClear();

    store.dispatch(logout());

    const state = store.getState();
    expect(state.auth.user.email).toBeNull();
    expect(state.user.user.email).toBe('');
    expect(state.chat.messages).toEqual([]);
    expect(state.cardInfo.cardName).toBe('');
    expect(state.noti.notifications).toEqual([]);
    expect(queryClient.getQueryData(['profile'])).toBeUndefined();
    expect(queryClient.getMutationCache().getAll()).toEqual([]);
    expect(deleteCache).toHaveBeenCalledWith('api-cache');
    expect(localStorage.getItem('token')).toBeNull();
    expect(localStorage.getItem('user')).toBeNull();
    expect(localStorage.getItem('locationSet')).toBeNull();

    store.dispatch(login({ email: 'b@haru.com', nickname: 'B', userId: 2, token: 'token-b' }));
    store.dispatch(setUser({ email: 'b@haru.com', nickname: 'B' }));

    const bState = store.getState();
    expect(bState.auth.user.email).toBe('b@haru.com');
    expect(bState.user.user.email).toBe('b@haru.com');
    expect(bState.chat.messages).toEqual([]);
    expect(bState.cardInfo.cardName).toBe('');
    expect(bState.noti.notifications).toEqual([]);
    expect(queryClient.getQueryData(['profile'])).toBeUndefined();
  });
});
