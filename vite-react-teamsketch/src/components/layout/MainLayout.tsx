import { Routes, Route, useLocation } from 'react-router-dom';
import { useEffect, useState, useLayoutEffect, useMemo } from 'react';
import MarketList from '../../pages/marketplace/MarketList';
import Login from '../../pages/account/Login';
import Signup from '../../pages/account/Signup';
import ServiceChat from '../../pages/mypage/AIChatBot';
import ShareLocationMap from '../../pages/map/ShareLocationMap';
import MyPage from '../../pages/mypage/MyPage';
import Setting from '../../pages/mypage/Setting';
import ForgotPassword from '../../pages/account/ForgotPassword';
import ProductRegister from '../../pages/marketplace/ProductRegister';
import VerifyMethod from '../../pages/account/VerifyMethod';
import VerficationCode from '../../pages/account/VerificationCode';
import ResetPassword from '../../pages/account/ResetPassword';
import ChatList from '../../pages/chatroom/ChatList';
import ChatRoom from '../../pages/chatroom/ChatRoom';
import ProdLocationMap from '../../pages/marketplace/ProdLocationMap';
import ChangePassword from '../../pages/account/ChangePassword';
import ProductDetails from '../../pages/marketplace/ProductDetails';
import ProfileManage from '../../pages/mypage/ProfileManage';
import NotificationList from '../../pages/notification/NotificationList';
import NotificationSetting from '../../pages/notification/NotificationSetting';
import RegisteredCard from '../../pages/payment/RegisteredCard';
import CardDetails from '../../pages/payment/CardDetails';
import TransactionList from '../../pages/Transaction history/TrasactionList';
import SalesList from '../../pages/Transaction history/SalesList';
import PurchaseList from '../../pages/Transaction history/PurchaseList';
import MyProducts from '../../pages/mypage/MyProducts';
import TransactionDetail from '../../pages/Transaction history/TransactionDetail';
import CSList from '../../pages/CScenter/CSList';
import InquiryHistory from '../../pages/CScenter/InquiryHistory';
import DeleteAccount from '../../pages/account/DeleteAccount';
import OCRUpload from '../../pages/payment/OCRUpload';
import MyLocation from '../../pages/map/MyLocation';

import RegistersList from '../features/list/RegistersList';
import RequestsList from '../features/list/RequestsList';
//================== Test =========================
import TestComponent from '../../testpages/TestComponent';
import TestPages from '../../testpages/TestPages';
import TestFunc from '../../testpages/TestFunc';
import TestGrid from '../../testpages/TestGrid';
import SimpleChatExample from '../../components/features/chat/SimpleChatExample';

// 전체 화면 (헤더/푸터 제외) 경로 목록
export const FULLSCREEN_PATHS = ['/product/location'];

// 푸터만 숨기는 경로 목록
export const FOOTER_HIDDEN_PATHS = [
  '/servicechat',
  '/chat/',
  '/sharelocation',
  '/change-password',
  '/forgotpassword',
  '/verfication-code',
  '/verify-method',
  '/reset-password'
];

// 로그인/회원가입 관련 인증 경로
export const AUTH_PATHS = ['/login', '/signup'];

// 첫 로그인 시 위치 설정 페이지인지 확인하는 함수
export const isInitialLocationPage = () => {
  const token = localStorage.getItem('token');
  const locationSet = localStorage.getItem('locationSet');
  const path = window.location.pathname;

  return token && !locationSet && path === '/my-location';
};

const MainLayout = () => {
  const [footerHeight, setFooterHeight] = useState<number>(0);
  const [headerHeight, setHeaderHeight] = useState<number>(0);
  const location = useLocation();

  // 스크롤 및 패딩 제외할 경로 목록 (전체 화면 컴포넌트)
  const fullscreenPaths = useMemo(() => FULLSCREEN_PATHS, []);

  // 푸터를 숨기는 경로 목록
  const footerHiddenPaths = useMemo(() => FOOTER_HIDDEN_PATHS, []);

  // 현재 경로가 전체 화면 경로인지 확인
  const isFullscreenPage = useMemo(
    () => fullscreenPaths.some((path) => location.pathname.includes(path)),
    [location.pathname, fullscreenPaths]
  );

  // 현재 경로가 푸터만 숨기는 경로인지 확인
  const isFooterHiddenPage = useMemo(
    () => footerHiddenPaths.some((path) => location.pathname.startsWith(path)),
    [location.pathname, footerHiddenPaths]
  );

  // 첫 로그인 시 위치 설정 페이지 여부 확인
  const isInitialLocation = useMemo(() => {
    return (
      localStorage.getItem('token') &&
      !localStorage.getItem('locationSet') &&
      location.pathname === '/my-location'
    );
  }, [location.pathname]);

  // 헤더와 푸터 높이 계산 함수
  const updateHeights = () => {
    const footer = document.getElementById('main-footer');
    const header = document.getElementById('main-header');

    if (footer) {
      setFooterHeight(footer.offsetHeight);
      document.documentElement.style.setProperty('--footer-height', `${footer.offsetHeight}px`);
    }
    if (header) {
      setHeaderHeight(header.offsetHeight);
      document.documentElement.style.setProperty('--header-height', `${header.offsetHeight}px`);
    }
  };

  // 초기 렌더링 시 레이아웃 요소 높이 계산 (useLayoutEffect)
  useLayoutEffect(() => {
    updateHeights();
  }, []);

  // 컴포넌트 마운트 및 경로 변경 시 높이 계산
  useEffect(() => {
    // 초기 높이 계산
    updateHeights();

    // DOM이 완전히 렌더링된 후 다시 계산 (타이밍 문제 해결)
    const timer1 = setTimeout(() => {
      updateHeights();
    }, 100);

    // 약간 더 지연된 추가 계산 (애니메이션 완료 후)
    const timer2 = setTimeout(() => {
      updateHeights();
    }, 300);

    // resize 이벤트 리스너 등록
    window.addEventListener('resize', updateHeights);

    return () => {
      window.removeEventListener('resize', updateHeights);
      clearTimeout(timer1);
      clearTimeout(timer2);
    };
  }, [location.pathname]); // 경로가 변경될 때마다 다시 계산

  // 패딩 계산
  const contentStyle = useMemo(() => {
    // 전체 화면 모드나 첫 로그인 위치 설정 페이지
    if (isInitialLocation || isFullscreenPage) {
      return {
        height: '100vh',
        paddingBottom: '0',
        paddingTop: '0'
      };
    }

    // 푸터만 숨기는 페이지
    if (isFooterHiddenPage) {
      return {
        height: `calc(100vh - env(safe-area-inset-bottom))`,
        paddingBottom: '0', // 푸터 패딩 제거
        paddingTop: `${headerHeight}px`,
        transition: 'padding 0.2s ease-in-out'
      };
    }

    // 일반 페이지
    return {
      height: `calc(100vh - env(safe-area-inset-bottom))`,
      paddingBottom: `${footerHeight}px`,
      paddingTop: `${headerHeight}px`,
      transition: 'padding 0.2s ease-in-out'
    };
  }, [footerHeight, headerHeight, isInitialLocation, isFullscreenPage, isFooterHiddenPage]);

  return (
    <main className="fixed inset-0 w-full">
      <div
        className={!isFullscreenPage && !isInitialLocation ? 'content-scroll' : ''}
        style={contentStyle}
      >
        <Routes>
          <Route path="/" element={<MarketList />} />
          <Route path="/login" element={<Login />} />
          <Route path="/signup" element={<Signup />} />
          <Route path="/servicechat" element={<ServiceChat />} />
          <Route path="/sharelocation/:chatroomId/:chatname" element={<ShareLocationMap />} />
          <Route path="/mypage" element={<MyPage />} />
          <Route path="/notification" element={<NotificationList />} />
          <Route path="/notification-setting" element={<NotificationSetting />} />
          <Route path="/setting" element={<Setting />} />
          <Route path="/forgotpassword" element={<ForgotPassword />} />
          <Route path="/product/register" element={<ProductRegister />} />
          <Route path="/verify-method" element={<VerifyMethod />} />
          <Route path="/reset-password" element={<ResetPassword />} />
          <Route path="/verfication-code" element={<VerficationCode />} />
          <Route path="/chat-list" element={<ChatList />} />
          <Route path="/product/location" element={<ProdLocationMap />} />
          <Route path="/chat/:chatroomId/:chatname" element={<ChatRoom />} />
          <Route path="/change-password" element={<ChangePassword />} />
          <Route path="/profile-manage" element={<ProfileManage />} />
          <Route path="/product-details" element={<ProductDetails />} />
          <Route path="/registered-card" element={<RegisteredCard />} />
          <Route path="/card-details/:cardId" element={<CardDetails />} />
          <Route path="/transaction-list" element={<TransactionList />} />
          <Route path="/sales-list" element={<SalesList />} />
          <Route path="/purchase-list" element={<PurchaseList />} />
          <Route path="/my-products" element={<MyProducts />} />
          <Route path="/transaction-detail" element={<TransactionDetail />} />
          <Route path="/cs-list" element={<CSList />} />
          <Route path="/inquiry-history" element={<InquiryHistory />} />
          <Route path="/delete-account" element={<DeleteAccount />} />
          <Route path="/ocr-upload" element={<OCRUpload />} />
          <Route path="/my-location" element={<MyLocation />} />

          <Route path="/registers-list" element={<RegistersList />} />
          <Route path="/requests-list" element={<RequestsList />} />

          {/* test pages */}
          <Route path="/test/pages" element={<TestPages />} />
          <Route path="/test/component" element={<TestComponent />} />
          <Route path="/test/func" element={<TestFunc />} />
          <Route path="/test/grid" element={<TestGrid />} />
          <Route
            path="/test/chat"
            element={
              <SimpleChatExample chatroomId={1} userEmail="test@test.com" token="testToken" />
            }
          />
        </Routes>
      </div>
    </main>
  );
};

export default MainLayout;
