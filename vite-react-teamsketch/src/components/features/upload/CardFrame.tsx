import React from 'react';

interface CardFrameProps {
  guideText?: string;
  className?: string;
  isDetected?: boolean;
  isLoading?: boolean;
  loadingProgress?: number;
  detectionStatus?: string;
}

const CardFrame: React.FC<CardFrameProps> = ({
  guideText = '',
  className = '',
  isDetected = false,
  isLoading = false,
  loadingProgress = 0,
  detectionStatus = ''
}) => {
  const isMobile = /iPhone|iPad|iPod|Android/i.test(navigator.userAgent);
  const cardRatio = 85.6 / 53.98;  // 실제 신용카드 비율 (가로/세로)
  
  return (
    <div
      className={`absolute inset-0 flex items-center justify-center pointer-events-none ${className}`}
    >
      <div className={`relative ${
        isMobile 
        ? 'w-[85vw]'  // 모바일에서는 화면 너비의 85%로 조정
        : 'w-[400px]'  // 데스크톱에서는 400px로 설정
      }`} style={{ 
        height: isMobile ? `calc(85vw / ${cardRatio})` : `calc(400px / ${cardRatio})` 
      }}>        

        {/* 카드 프레임 영역 */}
        <div className="absolute inset-0 rounded-2xl overflow-hidden">
          {/* 전체 테두리 */}
          <div className={`absolute inset-0 border-4 rounded-2xl transition-colors duration-300 ${
            isDetected 
              ? 'border-green-500/90' 
              : 'border-white/90'
          }`} />
          
          {/* 상단 가로 라인 */}
          <div className={`absolute top-0 left-8 right-8 h-[2px] transition-colors duration-300 ${
            isDetected 
              ? 'bg-green-500/80' 
              : 'bg-white/80'
          }`} />
          
          {/* 하단 가로 라인 */}
          <div className={`absolute bottom-0 left-8 right-8 h-[2px] transition-colors duration-300 ${
            isDetected 
              ? 'bg-green-500/80' 
              : 'bg-white/80'
          }`} />
          
          {/* 좌측 세로 라인 */}
          <div className={`absolute top-8 bottom-8 left-0 w-[2px] transition-colors duration-300 ${
            isDetected 
              ? 'bg-green-500/80' 
              : 'bg-white/80'
          }`} />
          
          {/* 우측 세로 라인 */}
          <div className={`absolute top-8 bottom-8 right-0 w-[2px] transition-colors duration-300 ${
            isDetected 
              ? 'bg-green-500/80' 
              : 'bg-white/80'
          }`} />
        </div>

        {/* 상태 표시 영역 */}
        <div className="absolute -top-20 left-0 right-0 flex flex-col items-center gap-2">
          {/* OpenCV 로딩 상태 */}
          {isLoading && (
            <div className="bg-black/50 text-white px-4 py-2 rounded-full text-sm flex items-center gap-2">
              <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
              <span>OpenCV.js 로딩 중... {loadingProgress}%</span>
            </div>
          )}
          
          {/* 감지 상태 */}
          {detectionStatus && (
            <div className={`px-4 py-2 rounded-full text-sm ${
              isDetected 
                ? 'bg-green-500/80 text-white' 
                : 'bg-black/50 text-white'
            }`}>
              {detectionStatus}
            </div>
          )}

          {/* 가이드 텍스트 */}
          <p className={`text-sm font-medium transition-colors duration-300 ${
            isDetected 
              ? 'text-green-500' 
              : 'text-white'
          }`}>
            {guideText}
          </p>
          <div className={`mt-1 w-16 h-1.5 rounded-full transition-colors duration-300 ${
            isDetected 
              ? 'bg-green-500/40' 
              : 'bg-white/40'
          }`} />
        </div>
      </div>
    </div>
  );
};

export default CardFrame;
