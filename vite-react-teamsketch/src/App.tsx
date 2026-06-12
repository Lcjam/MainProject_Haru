import { Routes, Route, useNavigate, useLocation } from 'react-router-dom';
import { useEffect, useMemo, useState, memo } from 'react';
import Header from './components/layout/Header';
import Footer from './components/layout/Footer';
import PrivateRoute from './routes/PrivateRoute';
import Login from './pages/account/Login';
import Signup from './pages/account/Signup';
import MainLayout from './components/layout/MainLayout';
import { useCategories } from './hooks/useCategories';
import { ToastContainer, toast } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import {
  AUTH_PATHS,
  FULLSCREEN_PATHS,
  FOOTER_HIDDEN_PATHS,
  isInitialLocationPage
} from './components/layout/MainLayout';
import { WebSocketProvider } from './contexts/WebSocketContext';
import { useNotification } from './services/real-time/useNotification';
import { useAppDispatch } from './store/hooks';
import { addNotification, updateLastProcessedId, INotification, updateUnreadChatCount } from './store/slices/notiSlice';
import ChangePassword from './pages/account/ChangePassword';
import ForgotPassword from './pages/account/ForgotPassword';
import VerificationCode from './pages/account/VerificationCode';
import VerifyMethod from './pages/account/VerifyMethod';
import ResetPassword from './pages/account/ResetPassword';
import DeleteAccount from './pages/account/DeleteAccount';
import SplashScreen from './components/common/splash/SplashScreen';

const App = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const dispatch = useAppDispatch();
  

  // 토큰 및 위치 설정 여부 확인
  const token = localStorage.getItem('token') || undefined;
  const locationSet = localStorage.getItem('locationSet');

  const user = localStorage.getItem('user'); // 사용자 정보 가져오기
  const userEmail = JSON.parse(user || '{}').email; // 이메일 추출
  console.log('user', user);
  // 헤더/푸터를 표시할지 여부 결정
  const shouldShowHeader = useMemo(() => {
    // 인증 페이지에서는 표시하지 않음
    if (AUTH_PATHS.includes(location.pathname)) return false;

    // 전체 화면 경로에서는 표시하지 않음
    for (const path of FULLSCREEN_PATHS) {
      if (location.pathname.includes(path)) return false;
    }

    // 첫 로그인 위치 설정 페이지에서는 표시하지 않음
    if (isInitialLocationPage()) return false;

    return true;
  }, [location.pathname]);

  // 푸터를 표시할지 여부 결정
  const shouldShowFooter = useMemo(() => {
    // 헤더가 표시되지 않는 경우에는 푸터도 표시하지 않음
    if (!shouldShowHeader) return false;

    // 푸터를 숨기는 경로 체크
    for (const path of FOOTER_HIDDEN_PATHS) {
      if (location.pathname.startsWith(path)) return false;
    }

    // 내 위치 설정 화면에서는 첫 로그인 여부와 관계없이 항상 푸터를 표시하지 않음
    if (location.pathname === '/my-location') return false;

    return true;
  }, [shouldShowHeader, location.pathname]);

  useCategories();

  // 인증 라우팅 로직
  useEffect(() => {
    const publicPaths = [
      '/login',
      '/signup',
      '/change-password',
      '/forgotpassword',
      '/verfication-code',
      '/verify-method',
      '/reset-password'
    ];

    // 토큰이 없고 공개 경로가 아닌 경우 로그인 페이지로 이동
    if (!token && !publicPaths.includes(location.pathname)) {
      navigate('/login');
      return;
    }

    // 토큰이 있고 위치 설정이 안 된 경우 첫 로그인으로 간주하여 위치 설정 페이지로 이동
    if (token && !locationSet && location.pathname !== '/my-location') {
      navigate('/my-location');
      return;
    }
  }, [navigate, location.pathname, token, locationSet]);

  // 사용자가 로그인했을 때만 알림 구독 (메모이제이션 적용)
  const NotificationHandler = memo(() => {
    const location = useLocation();
    const isChatListPage = location.pathname.startsWith('/chat-list');
    
    // 현재 채팅방 ID 추출
    const currentChatroomId = useMemo(() => {
      const match = location.pathname.match(/\/chat\/(\d+)/);
      return match ? parseInt(match[1]) : null;
    }, [location.pathname]);

    // 로그인 정보 확인 로그 추가
    console.log('[알림 디버그] ==== NotificationHandler 시작 ====');
    console.log('[알림 디버그] 토큰 존재 여부:', !!token);
    console.log('[알림 디버그] 사용자 이메일:', userEmail);

    // 알림 상태 가져오기
    const { notifications, isConnected, connect } = useNotification({
      userEmail: userEmail || '',
      token: token
    });

    // 컴포넌트 마운트 시 연결 시도 (의존성 배열 최적화)
    useEffect(() => {
      console.log('[알림 디버그] NotificationHandler 마운트됨, 연결 시도...');
      if (userEmail && token) {
        console.log('[알림 디버그] 알림 서비스 연결 시도:', userEmail);
        connect();
      } else {
        console.error('[알림 디버그] 연결 불가: 인증 정보 누락');
      }

      // 컴포넌트 언마운트 시 자원 정리
      return () => {
        console.log('[알림 디버그] NotificationHandler 언마운트됨');
      };
    }, [connect]);

    // 마지막으로 처리한 알림의 ID를 저장
    const [lastProcessedId, setLastProcessedId] = useState<string>('');

    // 최신 알림이 오면 Redux store에 저장하고 토스트 메시지 표시
    useEffect(() => {
      if (notifications.length === 0 || !isConnected) return;

      // 최신 알림 가져오기
      const latestNotification = notifications[notifications.length - 1];

      // 타입 가드 함수 추가
      const isChatMessage = (notification: any): notification is INotification => {
        return notification.type === 'CHAT_MESSAGE' && notification.chatroomId !== undefined;
      };

      // 현재 채팅방의 메시지인 경우 알림 무시
      if (
        isChatMessage(latestNotification) && 
        currentChatroomId && 
        latestNotification.chatroomId === currentChatroomId
      ) {
        console.log('[알림 디버그] 현재 채팅방 메시지 무시:', latestNotification);
        return;
      }

      // 고유 ID 생성
      const notificationId = `${latestNotification.receiverEmail}-${latestNotification.message}-${
        latestNotification.timestamp || Date.now()
      }`;

      // 마지막으로 처리한 알림과 같은지 확인
      if (notificationId === lastProcessedId) {
        console.log('[알림 디버그] 이미 처리된 알림, 무시:', notificationId);
        return;
      }

      console.log('[알림 디버그] 새 알림 처리:', latestNotification);

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
      if (isChatMessage(latestNotification) && 
          (!currentChatroomId || latestNotification.chatroomId !== currentChatroomId)) {
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
        console.error('[알림 디버그] 토스트 메시지 표시 오류:', error);
      }
    }, [notifications, isConnected, lastProcessedId, currentChatroomId, dispatch, isChatListPage]);

    // WebSocket 연결 상태 모니터링
    useEffect(() => {
      console.log('[알림 디버그] WebSocket 연결 상태:', isConnected ? '연결됨' : '연결 안됨');
    }, [isConnected]);

    // 이 컴포넌트는 UI를 렌더링하지 않고 알림 처리만 담당
    return null;
  });

  // 사용자 정보 기반으로 NotificationHandler 마운트 여부 결정
  const showNotificationHandler = useMemo(() => {
    return !!(token && userEmail);
  }, [token, userEmail]);

  return (
    <WebSocketProvider token={token} autoConnect={!!token}>
      <SplashScreen />
      <div className="flex flex-col min-h-screen">
        {shouldShowHeader && <Header />}

        {/* 알림 핸들러 컴포넌트 - 메모이제이션 사용 */}
        {showNotificationHandler ? <NotificationHandler /> : null}

        <Routes>
          {/* Public Routes */}
          <Route path="/login" element={<Login />} />
          <Route path="/signup" element={<Signup />} />
          <Route path="/change-password" element={<ChangePassword />} />
          <Route path="/forgotpassword" element={<ForgotPassword />} />
          <Route path="/verfication-code" element={<VerificationCode />} />
          <Route path="/verify-method" element={<VerifyMethod />} />
          <Route path="/reset-password" element={<ResetPassword />} />
          <Route path="/delete-account" element={<DeleteAccount />} />

          {/* Protected Routes */}
          <Route
            path="/*"
            element={
              <PrivateRoute>
                <MainLayout />
              </PrivateRoute>
            }
          />
        </Routes>
        {shouldShowFooter && <Footer />}
        <ToastContainer
          position="top-center"
          autoClose={2000}
          hideProgressBar={false}
          newestOnTop={true}
          closeOnClick
          rtl={false}
          pauseOnFocusLoss
          draggable
          pauseOnHover
          theme="dark"
          limit={3}
          toastClassName={() =>
            'relative flex items-center rounded-lg shadow-lg bg-primary-500 text-white text-sm p-4 mb-4 mt-12'
          }
          progressClassName="bg-primary-500"
        />
      </div>
    </WebSocketProvider>
  );
};

export default App;
