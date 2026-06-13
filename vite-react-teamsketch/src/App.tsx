import { Routes, Route, useNavigate, useLocation } from 'react-router-dom';
import { useEffect, useMemo } from 'react';
import Header from './components/layout/Header';
import Footer from './components/layout/Footer';
import PrivateRoute from './routes/PrivateRoute';
import Login from './pages/account/Login';
import Signup from './pages/account/Signup';
import MainLayout from './components/layout/MainLayout';
import { useCategories } from './hooks/useCategories';
import { ToastContainer } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import {
  AUTH_PATHS,
  FULLSCREEN_PATHS,
  FOOTER_HIDDEN_PATHS,
  isInitialLocationPage
} from './components/layout/MainLayout';
import { WebSocketProvider } from './contexts/WebSocketContext';
import NotificationHandler from './services/real-time/NotificationHandler';
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

  // 토큰 및 위치 설정 여부 확인
  const token = localStorage.getItem('token') || undefined;
  const locationSet = localStorage.getItem('locationSet');

  const user = localStorage.getItem('user'); // 사용자 정보 가져오기
  const userEmail = JSON.parse(user || '{}').email; // 이메일 추출

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

  // 인증 라우팅 로직 (NotificationHandler는 services/real-time/로 분리됨)
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
        {showNotificationHandler ? (
          <NotificationHandler userEmail={userEmail} token={token} />
        ) : null}

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
