import React, { useState, useRef, useEffect } from 'react';
import { useChat } from '../../../services/real-time/useChat';
import { MessageType, IChatMessage } from '../../../services/real-time/types';

interface SimpleChatExampleProps {
  chatroomId: number;
  userEmail: string;
  token: string;
}

const SimpleChatExample: React.FC<SimpleChatExampleProps> = ({
  chatroomId,
  userEmail,
  token
}) => {
  const [inputMessage, setInputMessage] = useState('');
  const messagesEndRef = useRef<HTMLDivElement>(null);
  
  // 채팅 훅 사용
  const { messages, sendMessage, isConnected, connect } = useChat({
    chatroomId,
    userEmail,
    token
  });

  // 메시지 입력 변경 처리
  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setInputMessage(e.target.value);
  };

  // 메시지 전송 처리
  const handleSendMessage = (e: React.FormEvent) => {
    e.preventDefault();
    if (inputMessage.trim()) {
      sendMessage(chatroomId, inputMessage, MessageType.TEXT);
      setInputMessage('');
    }
  };

  // 이미지 첨부 처리
  const handleImageUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      // 여기서는 이미지 URL을 직접 전송하는 예제
      // 실제 구현에서는 이미지를 서버에 업로드한 후 URL을 받아 전송해야 함
      const imageUrl = URL.createObjectURL(file);
      sendMessage(chatroomId, imageUrl, MessageType.IMAGE);
    }
  };

  // 메시지 목록이 변경될 때마다 스크롤 맨 아래로 이동
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  return (
    <div className="flex flex-col h-[500px] w-full max-w-md mx-auto border rounded-lg shadow-md">
      {/* 채팅방 헤더 */}
      <div className="flex items-center justify-between p-3 border-b">
        <h2 className="text-lg font-semibold">채팅방 #{chatroomId}</h2>
        <div className="flex items-center space-x-2">
          <div className={`w-2 h-2 rounded-full ${isConnected ? 'bg-green-500' : 'bg-red-500'}`}></div>
          <span className="text-sm">{isConnected ? '연결됨' : '연결 끊김'}</span>
          <button
            onClick={connect}
            className={`px-2 py-1 text-xs text-white rounded ${
              isConnected ? 'bg-red-500' : 'bg-green-500'
            }`}
          >
            {isConnected ? '연결 해제' : '연결'}
          </button>
        </div>
      </div>

      {/* 메시지 영역 */}
      <div className="flex-1 p-4 overflow-y-auto">
        {messages.length === 0 ? (
          <div className="flex items-center justify-center h-full text-gray-400">
            아직 메시지가 없습니다.
          </div>
        ) : (
          messages.map((msg: IChatMessage, index: number) => (
            <div
              key={msg.messageId || index}
              className={`mb-3 ${
                msg.senderEmail === userEmail ? 'text-right' : 'text-left'
              }`}
            >
              <div
                className={`inline-block p-3 rounded-lg ${
                  msg.senderEmail === userEmail
                    ? 'bg-blue-500 text-white'
                    : 'bg-gray-200 text-gray-800'
                }`}
              >
                {msg.messageType === MessageType.IMAGE ? (
                  <img src={msg.content} alt="채팅 이미지" className="max-w-[200px]" />
                ) : (
                  <p>{msg.content}</p>
                )}
              </div>
              <div className="text-xs text-gray-500 mt-1">
                {msg.sentAt ? new Date(msg.sentAt).toLocaleTimeString() : '방금 전'}
              </div>
            </div>
          ))
        )}
        <div ref={messagesEndRef} />
      </div>

      {/* 메시지 입력 폼 */}
      <form onSubmit={handleSendMessage} className="flex items-center p-3 border-t">
        <label htmlFor="imageUpload" className="p-2 text-gray-500 cursor-pointer">
          <svg
            xmlns="http://www.w3.org/2000/svg"
            fill="none"
            viewBox="0 0 24 24"
            strokeWidth={1.5}
            stroke="currentColor"
            className="w-6 h-6"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              d="M2.25 15.75l5.159-5.159a2.25 2.25 0 013.182 0l5.159 5.159m-1.5-1.5l1.409-1.409a2.25 2.25 0 013.182 0l2.909 2.909m-18 3.75h16.5a1.5 1.5 0 001.5-1.5V6a1.5 1.5 0 00-1.5-1.5H3.75A1.5 1.5 0 002.25 6v12a1.5 1.5 0 001.5 1.5zm10.5-11.25h.008v.008h-.008V8.25zm.375 0a.375.375 0 11-.75 0 .375.375 0 01.75 0z"
            />
          </svg>
        </label>
        <input
          type="file"
          id="imageUpload"
          accept="image/*"
          className="hidden"
          onChange={handleImageUpload}
        />
        <input
          type="text"
          value={inputMessage}
          onChange={handleInputChange}
          className="flex-1 p-2 border rounded-l focus:outline-none"
          placeholder="메시지를 입력하세요..."
          disabled={!isConnected}
        />
        <button
          type="submit"
          className="p-2 text-white bg-blue-500 rounded-r hover:bg-blue-600 focus:outline-none disabled:bg-gray-400"
          disabled={!isConnected || !inputMessage.trim()}
        >
          전송
        </button>
      </form>
    </div>
  );
};

export default SimpleChatExample; 