import React from 'react';
import { MessageType } from '../../services/real-time/types';

interface ChatListItemProps {
  nickname: string;
  lastMessage?: string;
  time?: string;
  unreadCount: number;
  productImage: React.ReactNode;
  chatname: string;
  messageType?: MessageType;
  onClick: () => void;
}

const ChatListItem: React.FC<ChatListItemProps> = ({
  nickname,
  lastMessage,
  time,
  unreadCount,
  productImage,
  chatname,
  messageType = MessageType.TEXT,
  onClick
}) => {
  // 메시지 타입에 따른 표시 내용 결정
  const getMessagePreview = () => {
    switch (messageType) {
      case MessageType.IMAGE:
        return '📷 이미지를 보냈습니다.';
      case MessageType.LOCATION:
        return '📍 위치를 공유했습니다.';
      case MessageType.FILE:
        return '📎 파일을 보냈습니다.';
      case MessageType.SYSTEM:
        return '🔔 ' + lastMessage;
      default:
        return lastMessage;
    }
  };

  return (
    <div
      onClick={onClick}
      className="
        w-full bg-white dark:bg-gray-800 
        rounded-xl 
        shadow-sm hover:shadow-md
        transition-all duration-300
        p-3 mb-2
        flex items-center gap-3
        cursor-pointer
        border border-gray-100 dark:border-gray-700
        hover:border-primary-200 dark:hover:border-primary-700
      "
    >
      {/* 프로필 이미지 */}
      <div className="relative">
        <div className="w-12 h-12 rounded-full overflow-hidden ring-2 ring-primary-100 dark:ring-primary-900">
          {productImage}
        </div>
        {unreadCount > 0 && (
          <div
            className="
            absolute -top-1 -right-1
            bg-primary-500 text-white 
            rounded-full w-5 h-5 
            flex items-center justify-center 
            text-xs font-medium
            ring-2 ring-white dark:ring-gray-800
          "
          >
            {unreadCount}
          </div>
        )}
      </div>

      {/* 채팅 정보 */}
      <div className="flex-1 min-w-0">
        <div className="flex justify-between items-start mb-1">
          <div className="flex flex-col">
            <span className="font-medium text-gray-900 dark:text-white truncate">{nickname}</span>
            <span className="text-xs text-gray-500 dark:text-gray-400 truncate">{chatname}</span>
          </div>
          <span className="text-xs text-gray-500 dark:text-gray-400 whitespace-nowrap ml-2">
            {time}
          </span>
        </div>
        <p className={`text-sm truncate ${unreadCount > 0 ? 'text-primary-600 font-medium' : 'text-gray-600 dark:text-gray-300'}`}>
          {getMessagePreview()}
        </p>
      </div>
    </div>
  );
};

export default ChatListItem;
