import { memo, useEffect, useMemo, useState } from 'react';
import { useLocation } from 'react-router-dom';
import { toast } from 'react-toastify';
import { useNotification } from './useNotification';
import { useAppDispatch } from '../../store/hooks';
import {
  addNotification,
  updateLastProcessedId,
  updateUnreadChatCount,
  INotification
} from '../../store/slices/notiSlice';

interface NotificationHandlerProps {
  userEmail?: string;
  token?: string;
}

/**
 * 로그인 사용자의 실시간 알림 구독 핸들러.
 * UI를 렌더링하지 않고 STOMP 알림을 받아 Redux store/토스트로만 반영한다.
 * (App 내부 인라인 정의 시 매 렌더마다 재생성되던 문제를 막기 위해 모듈 스코프로 분리)
 */
const NotificationHandler: React.FC<NotificationHandlerProps> = memo(({ userEmail, token }) => {
  const dispatch = useAppDispatch();
  const location = useLocation();
  const isChatListPage = location.pathname.startsWith('/chat-list');

  // 현재 채팅방 ID 추출
  const currentChatroomId = useMemo(() => {
    const match = location.pathname.match(/\/chat\/(\d+)/);
    return match ? parseInt(match[1]) : null;
  }, [location.pathname]);

  // 알림 상태 가져오기
  const { notifications, isConnected, connect } = useNotification({
    userEmail: userEmail || '',
    token: token
  });

  // 컴포넌트 마운트 시 연결 시도
  useEffect(() => {
    if (userEmail && token) {
      connect();
    } else {
      console.error('[알림] 연결 불가: 인증 정보 누락');
    }
  }, [connect]);

  // 마지막으로 처리한 알림의 ID를 저장
  const [lastProcessedId, setLastProcessedId] = useState<string>('');

  // 최신 알림이 오면 Redux store에 저장하고 토스트 메시지 표시
  useEffect(() => {
    if (notifications.length === 0 || !isConnected) return;

    // 최신 알림 가져오기
    const latestNotification = notifications[notifications.length - 1];

    // 타입 가드 함수 추가
    const isChatMessage = (notification: unknown): notification is INotification => {
      if (typeof notification !== 'object' || notification === null) return false;
      const n = notification as Record<string, unknown>;
      return n.type === 'CHAT_MESSAGE' && n.chatroomId !== undefined;
    };

    // 현재 채팅방의 메시지인 경우 알림 무시
    if (
      isChatMessage(latestNotification) &&
      currentChatroomId &&
      latestNotification.chatroomId === currentChatroomId
    ) {
      return;
    }

    // 고유 ID 생성
    const notificationId = `${latestNotification.receiverEmail}-${latestNotification.message}-${
      latestNotification.timestamp || Date.now()
    }`;

    // 마지막으로 처리한 알림과 같은지 확인
    if (notificationId === lastProcessedId) {
      return;
    }

    // Redux store에 알림 저장
    const notification: INotification = {
      id: Date.now(),
      type: latestNotification.type || 'SYSTEM',
      message: latestNotification.message,
      timestamp: latestNotification.timestamp || new Date().toISOString(),
      status: 'UNREAD',
      receiverEmail: latestNotification.receiverEmail,
      ...(isChatMessage(latestNotification) && {
        chatroomId: latestNotification.chatroomId,
        senderEmail: latestNotification.senderEmail
      })
    };

    // CHAT_MESSAGE 타입이고 현재 채팅방이 아닌 경우에만 카운트 증가
    if (
      isChatMessage(latestNotification) &&
      (!currentChatroomId || latestNotification.chatroomId !== currentChatroomId)
    ) {
      dispatch(updateUnreadChatCount(1));
    }
    // 선택적 필드 추가
    if ('senderEmail' in latestNotification) {
      notification.senderEmail = latestNotification.senderEmail as string;
    }
    if ('productId' in latestNotification) {
      notification.productId = latestNotification.productId as number;
    }
    if ('chatroomId' in latestNotification) {
      notification.chatroomId = latestNotification.chatroomId as number;
    }
    dispatch(addNotification(notification));

    // 마지막 처리 ID 업데이트
    dispatch(updateLastProcessedId(notificationId));
    setLastProcessedId(notificationId);

    // 채팅 목록 페이지에서는 토스트 메시지 표시 안함
    const notShowToast = latestNotification.type === 'CHAT_MESSAGE' && isChatListPage;

    // 토스트 메시지 표시
    try {
      if (!notShowToast) {
        toast.info(latestNotification.message, {
          toastId: notificationId,
          position: 'top-center',
          autoClose: 2000,
          theme: 'dark'
        });
      }
    } catch (error) {
      console.error('[알림] 토스트 메시지 표시 오류:', error);
    }
  }, [notifications, isConnected, lastProcessedId, currentChatroomId, dispatch, isChatListPage]);

  // 이 컴포넌트는 UI를 렌더링하지 않고 알림 처리만 담당
  return null;
});

NotificationHandler.displayName = 'NotificationHandler';

export default NotificationHandler;
