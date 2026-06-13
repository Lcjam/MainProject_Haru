import React from 'react';
import MessageInput from './MessageInput';

interface ChatInputZoneProps {
  inputRef: React.RefObject<HTMLDivElement | null>;
  onSendMessage: (
    message: string,
    file?: { type: string; url: string; name?: string }
  ) => void;
  onFocus: () => void;
  onBlur: () => void;
}

/** 채팅방 하단 고정 입력 영역 (MessageInput 래퍼) */
const ChatInputZone: React.FC<ChatInputZoneProps> = ({
  inputRef,
  onSendMessage,
  onFocus,
  onBlur
}) => {
  return (
    <div
      ref={inputRef}
      className="absolute bottom-0 left-0 right-0 bg-white dark:bg-gray-800 shadow-lg border-t border-gray-200 dark:border-gray-700"
    >
      <div className="mx-auto max-w-screen-md">
        <MessageInput
          onSendMessage={onSendMessage}
          onFocus={onFocus}
          onBlur={onBlur}
        />
      </div>
    </div>
  );
};

export default ChatInputZone;
