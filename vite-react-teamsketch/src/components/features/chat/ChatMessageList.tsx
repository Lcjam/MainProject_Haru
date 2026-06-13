import React from 'react';
import { MessageType, IChatMessage } from '../../../services/real-time/types';
import ProfileImage from '../image/ProfileImage';

/** 메시지 시간 포맷팅 (배열/문자열 날짜 모두 허용, KST 표기) */
export const formatMessageTime = (dateStr: string | number[]): string => {
  try {
    let date: Date;

    if (Array.isArray(dateStr)) {
      // 배열 형식의 날짜 처리 [년, 월, 일, 시, 분, 초], 월은 0부터 시작
      const [year, month, day, hour, minute, second] = dateStr;
      date = new Date(year, month - 1, day, hour, minute, second || 0);
    } else {
      date = new Date(dateStr);
    }

    if (isNaN(date.getTime())) {
      console.error('유효하지 않은 날짜:', dateStr);
      return '시간 정보 없음';
    }

    return new Intl.DateTimeFormat('ko-KR', {
      hour: '2-digit',
      minute: '2-digit',
      hour12: true
    }).format(date);
  } catch (error) {
    console.error('날짜 변환 오류:', error);
    return '시간 정보 없음';
  }
};

interface ChatMessageListProps {
  messages: IChatMessage[];
  userEmail: string;
  otherUserName?: string;
  containerRef: React.RefObject<HTMLDivElement | null>;
  messagesEndRef: React.RefObject<HTMLDivElement | null>;
  onAreaClick: () => void;
}

/** 채팅 메시지 목록 표시 영역 (스크롤 컨테이너 + 버블 렌더링) */
const ChatMessageList: React.FC<ChatMessageListProps> = ({
  messages,
  userEmail,
  otherUserName,
  containerRef,
  messagesEndRef,
  onAreaClick
}) => {
  return (
    <div
      ref={containerRef}
      onClick={onAreaClick}
      className="flex-1 p-4 overflow-y-auto space-y-4 bg-gray-50 dark:bg-gray-900 pb-20"
    >
      {messages.map((msg, index) => (
        <div key={msg.messageId || index} className="group relative">
          <div className={`flex flex-col ${msg.senderEmail === userEmail ? 'items-end' : 'items-start'}`}>
            {/* 상대방 메시지일 때만 프로필 이미지와 닉네임 표시 */}
            {msg.senderEmail !== userEmail && (
              <div className="flex items-start space-x-2 mb-1">
                <div className="w-8 h-8 rounded-full overflow-hidden ring-2 ring-primary-100 dark:ring-primary-900 flex-shrink-0">
                  <ProfileImage nickname={otherUserName || ''} />
                </div>
                <span className="text-xs text-gray-600 dark:text-gray-400 mt-1">
                  {otherUserName || '알 수 없음'}
                </span>
              </div>
            )}

            {/* 메시지와 시간을 감싸는 컨테이너 */}
            <div className={`flex items-end gap-2 ${msg.senderEmail !== userEmail ? 'ml-10' : ''}`}>
              {/* 시간을 왼쪽에 표시 (내가 보낸 메시지일 경우) */}
              {msg.senderEmail === userEmail && (
                <span className="text-xs text-gray-500 flex-shrink-0">
                  {formatMessageTime(msg.sentAt || '')}
                </span>
              )}
              {/* 메시지 내용 */}
              <div className={`max-w-[80%] rounded-2xl p-3 shadow-md ${
                msg.senderEmail === userEmail
                  ? 'bg-primary-500 text-white rounded-tr-sm'
                  : 'bg-white dark:bg-gray-800 rounded-tl-sm'
              }`}>
                {msg.messageType === MessageType.IMAGE ? (
                  <img
                    src={msg.content}
                    alt="전송된 이미지"
                    className="w-full max-w-xs rounded-xl"
                  />
                ) : msg.messageType === MessageType.FILE ? (
                  <a
                    href={msg.content}
                    download
                    className="block p-3 bg-white/10 rounded-xl text-white hover:bg-white/20 transition-colors"
                  >
                    📄 첨부파일
                  </a>
                ) : (
                  <p className={msg.senderEmail === userEmail ? 'text-white' : 'text-gray-800'}>
                    {msg.content}
                  </p>
                )}
              </div>
              {/* 시간을 오른쪽에 표시 (상대방이 보낸 메시지일 경우) */}
              {msg.senderEmail !== userEmail && (
                <span className="text-xs text-gray-500 flex-shrink-0">
                  {formatMessageTime(msg.sentAt || '')}
                </span>
              )}
            </div>
          </div>
        </div>
      ))}
      <div ref={messagesEndRef} />
    </div>
  );
};

export default ChatMessageList;
