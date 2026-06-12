import { useRef, useEffect } from 'react';
import { useAppDispatch, useAppSelector } from '../../store/hooks';
import { receiveMessage, setError, sendMessage } from '../../store/slices/chatSlice';
import { useSendChatMessage } from '../../services/api/serviceChatAPI';
import Loading from '../../components/common/Loading';
import MessageInput from '../../components/features/chat/MessageInput';

const AIChatBot = () => {
  const dispatch = useAppDispatch();
  const { messages, isLoading } = useAppSelector((state) => state.chat);
  const { mutate: sendChatMessage } = useSendChatMessage();
  const chatContainerRef = useRef<HTMLDivElement>(null);

  // 메시지가 추가될 때마다 스크롤을 아래로 이동
  useEffect(() => {
    if (chatContainerRef.current) {
      chatContainerRef.current.scrollTop = chatContainerRef.current.scrollHeight;
    }
  }, [messages]);

  const handleSendMessage = async (message: string, file?: { type: string; url: string; name?: string }) => {
    if (!message.trim() && !file) return;

    try {
      dispatch(sendMessage(message));
      
      sendChatMessage(message, {
        onSuccess: (response) => {
          dispatch(receiveMessage(response));
        },
        onError: (err) => {
          const errorMessage =
            err instanceof Error ? err.message : 'API 통신 관련 문제가 발생했습니다.';
          dispatch(setError(errorMessage));
        }
      });
      
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : '알 수 없는 오류가 발생했습니다.';
      dispatch(setError(errorMessage));
    }
  };
  
  return (
    <div className="flex flex-col h-full w-full bg-gray-50 dark:bg-gray-900 overflow-hidden">
      {/* 서비스 정보 */}
      <div className="p-4 bg-primary-400 z-10">
        <h2 className="text-xl font-bold text-white">네이버 클라우드 + TinyLlama 고객센터</h2>
        <p className="text-sm text-violet-100 mt-1 opacity-90">AI 기반 고객 상담 서비스</p>
      </div>

      {/* 메시지 영역 */}
      <div
        ref={chatContainerRef}
        className="flex-1 p-4 overflow-y-auto bg-gray-50 dark:bg-gray-900 space-y-4"
      >
        {messages.map((message, index) => (
          <div
            key={index}
            className={`flex ${
              message.role === 'user' ? 'justify-end' : 'justify-start'
            }`}
          >
            <div
              className={`max-w-[80%] rounded-2xl p-4 shadow-sm transition-all duration-200 ${
                message.role === 'user'
                  ? 'bg-gradient-to-r from-violet-500 to-purple-500 text-white'
                  : 'bg-white dark:bg-gray-700 text-gray-800 dark:text-white hover:shadow-md'
              }`}
            >
              <p className="text-sm md:text-base whitespace-pre-wrap break-words leading-relaxed">
                {message.content}
              </p>
            </div>
          </div>
        ))}
        {isLoading && (
          <div className="flex justify-start">
            <div className="bg-white dark:bg-gray-700 rounded-2xl p-4 shadow-sm">
              <Loading />
            </div>
          </div>
        )}
      </div>

      <MessageInput onSendMessage={(message, file) => handleSendMessage(message, file)} />
      
    </div>
  );
};

export default AIChatBot;
