import React, { useRef } from 'react';
import { createPortal } from 'react-dom';
import CardFrame from './CardFrame';
import { useCardScanner } from './useCardScanner';

interface CameraModalProps {
  videoRef: React.RefObject<HTMLVideoElement | null>;
  onCapture: () => void;
  onClose: () => void;
  onError: (message: string) => void;
}

const CameraModal: React.FC<CameraModalProps> = ({ videoRef, onCapture, onClose, onError }) => {
  const captureButtonRef = useRef<HTMLButtonElement>(null);

  // 카메라 스트림 + OpenCV 카드 감지/자동 촬영 로직 (useCardScanner)
  const {
    loading,
    loadingProgress,
    cardDetected,
    detectionStatus,
    canvasRef,
    handleCapture,
    handleClose
  } = useCardScanner({ videoRef, onCapture, onClose, onError });

  return createPortal(
    <div className="fixed inset-0 z-50 bg-black flex flex-col">
      {/* 상단 버튼 영역 */}
      <div className="relative flex justify-between items-center p-4">
        <button
          onClick={handleClose}
          className="text-white bg-black/30 p-2 rounded-full"
        >
          <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </div>

      {/* 카메라 영역 */}
      <div className="flex-1 relative overflow-hidden flex items-center justify-center bg-black">
        <video
          ref={videoRef}
          autoPlay
          playsInline
          muted
          className="absolute w-full h-full object-cover"
          style={{ objectFit: 'cover' }}
        />
        <canvas
          ref={canvasRef}
          className="absolute inset-0 w-full h-full pointer-events-none"
        />

        {/* 로딩 상태 */}
        {loading && (
          <div className="absolute inset-0 flex flex-col items-center justify-center bg-black bg-opacity-50 text-white">
            <svg className="animate-spin h-10 w-10 mb-2" viewBox="0 0 24 24">
              <circle
                className="opacity-25"
                cx="12"
                cy="12"
                r="10"
                stroke="currentColor"
                strokeWidth="4"
              ></circle>
              <path
                className="opacity-75"
                fill="currentColor"
                d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
              ></path>
            </svg>
            <p>카메라 초기화 중...</p>
          </div>
        )}

        <CardFrame
          guideText="카드를 프레임에 맞춰주세요"
          isDetected={cardDetected}
          isLoading={loading}
          loadingProgress={loadingProgress}
          detectionStatus={detectionStatus}
        />
      </div>

      {/* 하단 촬영 버튼 영역 */}
      <div className="p-6 flex justify-center">
        <button
          ref={captureButtonRef}
          onClick={handleCapture}
          disabled={loading}
          className={`
            w-16 h-16 rounded-full flex items-center justify-center
            ${cardDetected ? 'bg-green-500' : 'bg-white'}
            ${loading ? 'opacity-50 cursor-not-allowed' : 'opacity-100'}
            transition-all duration-300
          `}
        >
          <div className="w-14 h-14 rounded-full border-4 border-black" />
        </button>
      </div>
    </div>,
    document.body
  );
};

export default CameraModal;
