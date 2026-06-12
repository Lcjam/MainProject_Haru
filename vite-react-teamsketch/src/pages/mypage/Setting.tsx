import ThemeToggle from '../../components/common/ThemeToggle';
import { useNavigate } from 'react-router-dom';
import LogoutModal from '../account/LogoutModal';
import { useState } from 'react';
import { useDispatch } from 'react-redux';
import { logout } from '../../store/slices/authSlice';
import { clearUser } from '../../store/slices/userSlice';
import { clearNotifications } from '../../store/slices/notiSlice';

const Setting = () => {
  const [isLogoutModalOpen, setLogoutModalOpen] = useState(false);
  const dispatch = useDispatch();
  const navigate = useNavigate();

  // 로그아웃 함수
  const handleLogout = () => {
    console.log('로그아웃 실행!');
    dispatch(logout());
    dispatch(clearUser());
    dispatch(clearNotifications());
    setLogoutModalOpen(false);
    navigate('/login');
  };

  // 비밀번호 변경 페이지 이동
  const handleChangePassword = () => {
    navigate('/change-password');
  };

  // 회원 탈퇴 페이지 이동
  const handleDeleteAccount = () => {
    navigate('/delete-account');
  };

  const handleNotification = () => {
    navigate('/notification-setting');
  }
  return (
    <div className="p-4 space-y-4">
      {/* 모드 변경 */}
      <div className="flex items-center justify-between p-2 bg-[#F3F2FF] dark:bg-[#1C1C1C] rounded-lg">
        <span className="text-[#4A4A4A] dark:text-white">모드 변경</span>
        <ThemeToggle />
      </div>

      {/* 알림 설정 */}
      <button
        onClick={handleNotification}
        className="w-full px-4 py-2 text-white bg-purple-300 hover:bg-purple-500 rounded-lg transition-colors"
      >
        알림 설정
      </button>

      {/* 로그아웃 */}
      <button
        onClick={() => setLogoutModalOpen(true)}
        className="w-full px-4 py-2 text-white bg-purple-300 hover:bg-purple-500 rounded-lg transition-colors"
      >
        로그아웃
      </button>

      {/* 비밀번호 변경 */}
      <button
        onClick={handleChangePassword}
        className="w-full px-4 py-2 text-white bg-purple-300 hover:bg-purple-500 rounded-lg transition-colors"
      >
        비밀번호 변경
      </button>

      {/* 회원탈퇴 */}
      <button
        onClick={handleDeleteAccount}
        className="w-full px-4 py-2 text-white bg-purple-300 hover:bg-purple-500 rounded-lg transition-colors"
      >
        회원탈퇴
      </button>

      {/* 로그아웃 모달 */}
      <LogoutModal
        isOpen={isLogoutModalOpen}
        onClose={() => setLogoutModalOpen(false)}
        onLogout={handleLogout}
      />
    </div>


  );
};

export default Setting;
