import { useState } from "react";
import { useAppSelector, useAppDispatch } from "../../store/hooks";
import { markAllNonChatAsRead, INotification } from "../../store/slices/notiSlice";
import { formatDistanceToNow } from 'date-fns';
import { ko } from 'date-fns/locale';

const NotificationList = () => {
  const [filter, setFilter] = useState("전체");
  const dispatch = useAppDispatch();
  const { notifications } = useAppSelector((state) => state.noti);

  // CHAT_MESSAGE를 제외한 알림만 필터링
  const nonChatNotifications = notifications.filter(n => n.type !== 'CHAT_MESSAGE');

  // 알림 필터링 (CHAT_MESSAGE 제외)
  const filteredNotifications = nonChatNotifications.filter((n: INotification) => {
    if (filter === "전체") return true;
    return n.type === filter;
  });

  // 필터 옵션에서도 CHAT_MESSAGE 제외
  const filterOptions = [
    "전체", 
    "PRODUCT_REQUEST", 
    "JOIN_REQUEST", 
    "LOCATION_SHARE", 
    "PRODUCT_APPROVAL"
  ];

  // 알림 읽음 처리
  const handleMarkAllAsRead = () => {
    dispatch(markAllNonChatAsRead());
  };

  // 알림 타입별 스타일
  const getNotificationStyle = (type: string) => {
    switch (type) {   
      case 'PRODUCT_REQUEST':
        return {
          icon: '🛍️',
          bgColor: 'bg-green-50 dark:bg-green-900/20',
          textColor: 'text-green-600 dark:text-green-300',
          borderColor: 'border-green-100 dark:border-green-800'
        };
      case 'JOIN_REQUEST':
        return {
          icon: '👥',
          bgColor: 'bg-purple-50 dark:bg-purple-900/20',
          textColor: 'text-purple-600 dark:text-purple-300',
          borderColor: 'border-purple-100 dark:border-purple-800'
        };
      case 'LOCATION_SHARE':
        return {
          icon: '📍',
          bgColor: 'bg-yellow-50 dark:bg-yellow-900/20',
          textColor: 'text-yellow-600 dark:text-yellow-300',
          borderColor: 'border-yellow-100 dark:border-yellow-800'
        };
      case 'PRODUCT_APPROVAL':
        return {
          icon: '✅',
          bgColor: 'bg-emerald-50 dark:bg-emerald-900/20',
          textColor: 'text-emerald-600 dark:text-emerald-300',
          borderColor: 'border-emerald-100 dark:border-emerald-800'
        };
      default:
        return {
          icon: '📢',
          bgColor: 'bg-gray-50 dark:bg-gray-900/20',
          textColor: 'text-gray-600 dark:text-gray-300',
          borderColor: 'border-gray-100 dark:border-gray-800'
        };
    }
  };

  return (
    <div className="flex flex-col h-screen bg-gray-50 dark:bg-gray-900">
      {/* 알림 유형 필터 */}
      <div className="sticky top-0 z-10 bg-white dark:bg-gray-800 shadow-sm">
        <div className="flex space-x-2 overflow-x-auto px-4 py-3 scrollbar-hide">
          {filterOptions.map((type) => (
            <button
              key={type}
              onClick={() => setFilter(type)}
              className={`
                px-4 py-2 rounded-full text-sm font-medium whitespace-nowrap
                transition-all duration-200 ease-in-out
                ${filter === type 
                  ? 'bg-primary-500 text-white shadow-md hover:bg-primary-600' 
                  : 'bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600'
                }
              `}
            >
              {type}
            </button>
          ))}
        </div>
      </div>

      {/* 읽지 않은 알림 카운터 (CHAT_MESSAGE 제외) */}
      <div className="bg-white dark:bg-gray-800 px-4 py-3 border-b border-gray-100 dark:border-gray-700">
        <div className="flex justify-between items-center">
          <span className="text-sm font-medium text-gray-600 dark:text-gray-300">
            읽지 않은 알림 <span className="text-primary-500">
              {nonChatNotifications.filter(n => n.status === 'UNREAD').length}
            </span>개
          </span>
          {nonChatNotifications.some(n => n.status === 'UNREAD') && (
            <button
              onClick={handleMarkAllAsRead}
              className="text-sm text-white hover:text-primary-600 dark:text-primary-400 dark:hover:text-primary-300 font-medium"
            >
              모두 읽음
            </button>
          )}
        </div>
      </div>

      {/* 알림 목록 */}
      <div className="flex-1 overflow-y-auto">
        <div className="p-4 space-y-3">
          {filteredNotifications.map((notification) => {
            const style = getNotificationStyle(notification.type);
            return (
              <div
                key={notification.id}
                onClick={() => notification.status === 'UNREAD' && handleMarkAllAsRead()}
                className={`
                  p-4 rounded-xl transition-all duration-200 cursor-pointer
                  ${notification.status === "UNREAD" 
                    ? `${style.bgColor} shadow-sm` 
                    : 'bg-white dark:bg-gray-800 shadow-sm'}
                  border ${style.borderColor}
                  hover:shadow-md hover:scale-[1.01] transform
                `}
              >
                <div className="flex items-start gap-3">
                  <div className={`
                    w-10 h-10 rounded-full flex items-center justify-center
                    ${style.bgColor} ${style.textColor}
                    text-xl
                  `}>
                    {style.icon}
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className={`${style.textColor} font-medium mb-1 line-clamp-2`}>
                      {notification.message}
                    </p>
                    <div className="flex justify-between items-center mt-2">
                      <span className={`
                        text-xs px-2 py-1 rounded-full
                        ${notification.status === "UNREAD" 
                          ? `${style.bgColor} ${style.textColor}` 
                          : 'bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-300'}
                      `}>
                        {notification.type}
                      </span>
                      <small className="text-gray-500 dark:text-gray-400 text-xs">
                        {formatDistanceToNow(new Date(notification.timestamp), { 
                          addSuffix: true,
                          locale: ko 
                        })}
                      </small>
                    </div>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
};

export default NotificationList;
