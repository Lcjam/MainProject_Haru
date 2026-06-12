import React, { useState, useRef } from "react";
import { FaPaperclip, FaPaperPlane } from "react-icons/fa";

interface MessageInputProps {
  onSendMessage?: (message: string, file?: { type: string; url: string; name?: string }) => void;
  onFocus?: () => void;
  onBlur?: () => void;
}

const MessageInput: React.FC<MessageInputProps> = ({onSendMessage, onFocus, onBlur }) => {
  const [message, setMessage] = useState("");
  const [file, setFile] = useState<{ type: string; url: string; name?: string } | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setMessage(e.target.value);
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const selectedFile = e.target.files?.[0];
    if (selectedFile) {
      const reader = new FileReader();
      reader.onloadend = () => {
        setFile({
          type: selectedFile.type.startsWith('image/') ? 'image' : 'file',
          url: reader.result as string,
          name: selectedFile.name
        });
      };
      reader.readAsDataURL(selectedFile);
    }
  };

  const handleSend = () => {
    if (!message.trim() && !file) return;
    onSendMessage?.(message, file || undefined);
    setMessage("");
    setFile(null);
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  return (
    <div>
      {/* 파일 미리보기 */}
      {file && (
        <div className="relative w-40">
          {/* 이미지 파일 미리보기 */}
          {file.type === "image" && (
            <img
              src={file.url}
              alt="이미지 미리보기"
              className="w-full h-32 object-cover rounded-md shadow-md"
            />
          )}
          {/* 파일 미리보기 */}
          {file.type === "file" && (
            <div className="p-3.5 ml-1 mr-1 border rounded-md text-sm bg-secondary-light">
              📄 {file.name}
            </div>
          )}

          {/* 파일 삭제 버튼 */}
          <button
            onClick={() => setFile(null)}
            className="absolute top-1 right-1 hover:bg-secondary-light font-bold bg-secondary-light text-secondary-dark text-xs px-2 py-1 rounded-full"
          >
            X
          </button>
        </div>
      )}
      
      {/* 메시지 입력 및 전송 영역 */}
      <div className="flex items-center bg-white shadow-md pb-5 pt-3 border-t border-primary-200 dark:border-border-dark">
        {/* 파일 선택 버튼 */}
        <button
          type="button"
          onClick={() => fileInputRef.current?.click()}
          className="p-2 text-gray-500 hover:text-primary-500 transition-colors"
        >
          <FaPaperclip className="w-5 h-5" />
        </button>
        <input
          type="file"
          ref={fileInputRef}
          onChange={handleFileChange}
          className="hidden"
          accept="image/*,.pdf,.doc,.docx,.txt"
        />

        {/* 메시지 입력창 */}
        <div className="flex-1 ml-2 mr-3 relative">
          <input
            type="text"
            value={message}
            onChange={handleChange}
            onKeyDown={handleKeyDown}
            className="w-full p-2 pr-12 border rounded-full"
            placeholder="메시지를 입력하세요..."
            onFocus={onFocus}
            onBlur={onBlur}
          />
          {/* 전송 버튼 */}
          {(message.trim() || file) && (
            <button
              onClick={handleSend}
              className="absolute right-1 top-1/2 -translate-y-1/2 text-white hover:text-primary-600 transition-colors rounded-full p-2"
            >
              <FaPaperPlane className="w-5 h-5" />
            </button>
          )}
        </div>
      </div>
    </div>
  );
};

export default MessageInput;