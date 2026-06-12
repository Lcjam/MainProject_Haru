import { useNavigate, useLocation } from 'react-router-dom';
import { RiStore2Line, RiStore2Fill } from 'react-icons/ri';
import {
  IoChatbubbleEllipsesOutline,
  IoChatbubbleEllipses,
  IoNotificationsOutline,
  IoNotifications
} from 'react-icons/io5';
import { HiOutlineMapPin, HiMapPin } from 'react-icons/hi2';
import { CgUser } from 'react-icons/cg';
import { RiUserFill } from 'react-icons/ri';
import { useAppDispatch, useAppSelector } from '../../store/hooks';
import { updateUnreadChatCount } from '../../store/slices/notiSlice';

const Footer = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { notifications, unreadChatCount } = useAppSelector((state) => state.noti);
  const unreadChatMessages = notifications.filter(n => n.type === 'CHAT_MESSAGE' && n.status === 'UNREAD').length;
  const dispatch = useAppDispatch();
  dispatch(updateUnreadChatCount(unreadChatMessages));

  // CHAT_MESSAGE를 제외한 읽지 않은 알림 수 계산
  const unreadNonChatCount = notifications.filter(
    n => n.status === 'UNREAD' && n.type !== 'CHAT_MESSAGE'
  ).length;

  const getPathForLabel = (label: string) => {
    switch (label) {
      case '마켓':
        return '/';
      case '채팅':
        return '/chat';
      case '알림':
        return '/notification';
      case '위치':
        return '/location';
      case '마이':
        return '/mypage';
      default:
        return '/';
    }
  };

  const navigationItems = [
    {
      icon: location.pathname === '/' ? <RiStore2Fill /> : <RiStore2Line />,
      label: '마켓',
      onClick: () => navigate('/')
    },
    {
      icon:
        location.pathname === '/chat-list' ? <IoChatbubbleEllipses /> : <IoChatbubbleEllipsesOutline />,
      label: '채팅',
      onClick: () => navigate('/chat-list'),
      badge: unreadChatCount > 0 ? unreadChatCount : undefined
    },
    {
      icon:
        location.pathname === '/notification' ? <IoNotifications /> : <IoNotificationsOutline />,
      label: '알림',
      onClick: () => navigate('/notification'),
      // CHAT_MESSAGE를 제외한 알림 수만 표시
      badge: unreadNonChatCount > 0 ? unreadNonChatCount : undefined
    },
    {
      icon: location.pathname === '/my-location' ? <HiMapPin /> : <HiOutlineMapPin />,
      label: '위치',
      onClick: () => navigate('/my-location')
    },
    {
      icon: location.pathname === '/mypage' ? <RiUserFill /> : <CgUser />,
      label: '마이',
      onClick: () => navigate('/mypage')
    }
  ];

  return (
    <footer className="fixed bottom-0 left-0 right-0 z-50 bg-primary-500 dark:bg-background-dark dark:bg-opacity-90">
      <nav
        className="container mx-auto flex justify-around items-center px-3 py-2"
        id="main-footer"
      >
        {navigationItems.map((item, index) => (
          <button
            key={index}
            onClick={item.onClick}
            className="flex flex-col items-center pb-5 gap-1 bg-transparent hover:bg-transparent focus:outline-none relative"
          >
            <div
              className={`text-3xl ${
                location.pathname === getPathForLabel(item.label)
                  ? 'text-primary-dark'
                  : 'text-light'
              } group-hover:scale-110 transition-transform duration-300`}
            >
              {item.icon}
              {item.badge && item.badge > 0 && (
                <div className="absolute -top-0 -right-0 bg-red-500 text-white text-xs rounded-full w-5 h-5 flex items-center justify-center">
                  {item.badge > 99 ? '99+' : item.badge}
                </div>
              )}
            </div>
          </button>
        ))}
      </nav>
    </footer>
  );
};

export default Footer;
