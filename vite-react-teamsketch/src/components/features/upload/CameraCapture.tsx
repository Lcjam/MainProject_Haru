import React, { useRef, useState, useEffect } from 'react';
import Button from '../../../components/common/Button';
import CameraModal from './CameraModal';
import { resetOpenCV } from '../../../utils/cardDetection';

interface CameraCaptureProps {
  onCapture: (file: File) => void;
  className?: string;
  onError?: (message: string) => void;
}

const CameraCapture: React.FC<CameraCaptureProps> = ({ onCapture, className = '', onError }) => {
  const [isActive, setIsActive] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const videoRef = useRef<HTMLVideoElement>(null);
  const streamRef = useRef<MediaStream | null>(null);

  const stopCamera = () => {
    if (streamRef.current) {
      console.log('카메라 스트림 정리 - CameraCapture');
      streamRef.current.getTracks().forEach((track) => {
        track.stop();
        console.log(`트랙 정리: ${track.kind}`);
      });
      streamRef.current = null;
    }
    
    if (videoRef.current) {
      videoRef.current.srcObject = null;
      console.log('비디오 소스 제거됨 - CameraCapture');
    }
    
    setIsActive(false);
    setErrorMessage(null);
  };

  const startCamera = async () => {
    try {
      console.log('카메라 시작 시도 - CameraCapture');
      setErrorMessage(null);
      
      // 이미 활성화된 경우 리소스 정리 후 재시작
      if (isActive || streamRef.current) {
        stopCamera();
        // OpenCV 리셋
        try {
          console.log('OpenCV 상태 리셋 시도...');
          resetOpenCV();
          console.log('기존 리소스 정리 후 재시작');
        } catch (resetError) {
          console.warn('OpenCV 리셋 중 오류 발생:', resetError);
          // 리셋 오류는 무시하고 계속 진행
        }
      }

      if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
        const msg = '이 브라우저는 카메라를 지원하지 않습니다. 최신 브라우저(Chrome, Firefox, Edge)를 사용해주세요.';
        setErrorMessage(msg);
        onError?.(msg);
        throw new Error(msg);
      }

      // 모달 활성화
      setIsActive(true);
    } catch (error) {
      console.error('카메라 초기화 실패:', error);
      setIsActive(false);
      
      // 사용자에게 친절한 오류 메시지 표시
      let errorMsg = '카메라를 시작할 수 없습니다.';
      
      if (error instanceof Error) {
        errorMsg = error.message;
      } else if (error instanceof DOMException) {
        // 권한 오류 처리
        if (error.name === 'NotAllowedError' || error.name === 'PermissionDeniedError') {
          errorMsg = '카메라 접근 권한이 거부되었습니다. 브라우저 설정에서 카메라 권한을 허용해주세요.';
        } else if (error.name === 'NotFoundError' || error.name === 'DevicesNotFoundError') {
          errorMsg = '카메라를 찾을 수 없습니다. 카메라가 연결되어 있는지 확인해주세요.';
        } else if (error.name === 'NotReadableError' || error.name === 'TrackStartError') {
          errorMsg = '카메라에 접근할 수 없습니다. 다른 앱에서 카메라를 사용 중인지 확인해주세요.';
        } else if (error.name === 'OverconstrainedError') {
          errorMsg = '카메라가 요청한 설정을 지원하지 않습니다. 다른 카메라를 사용해보세요.';
        } else if (error.name === 'TypeError') {
          errorMsg = '카메라 설정 오류. 브라우저를 새로고침해주세요.';
        }
      }
      
      setErrorMessage(errorMsg);
      onError?.(errorMsg);
      alert(errorMsg);
    }
  };

  const capturePhoto = () => {
    if (videoRef.current) {
      const canvas = document.createElement('canvas');
      canvas.width = videoRef.current.videoWidth;
      canvas.height = videoRef.current.videoHeight;
      const ctx = canvas.getContext('2d');

      if (ctx) {
        ctx.drawImage(videoRef.current, 0, 0);
        canvas.toBlob(
          (blob) => {
            if (blob) {
              const file = new File([blob], 'card-photo.jpg', { type: 'image/jpeg' });
              onCapture(file);
            } else {
              const msg = '이미지 캡처에 실패했습니다. 다시 시도해주세요.';
              setErrorMessage(msg);
              onError?.(msg);
            }
          },
          'image/jpeg',
          0.95
        );
      } else {
        const msg = '캔버스 컨텍스트를 생성할 수 없습니다.';
        setErrorMessage(msg);
        onError?.(msg);
      }
    }
  };

  // 모달 닫기 핸들러
  const handleCloseModal = () => {
    stopCamera();
    // 명시적 OpenCV 리셋
    resetOpenCV();
    console.log('모달 닫기 - 리소스 정리 완료');
  };

  // 컴포넌트 언마운트 시 카메라 정리
  useEffect(() => {
    return () => {
      stopCamera();
      resetOpenCV();
      console.log('컴포넌트 언마운트 - 모든 리소스 정리');
    };
  }, []);

  // 에러 메시지 변경 시 부모 컴포넌트에 알림
  useEffect(() => {
    if (errorMessage) {
      onError?.(errorMessage);
    }
  }, [errorMessage, onError]);

  return (
    <div>
      <Button
        variant="outline"
        onClick={startCamera}
        className={`flex items-center gap-2 ${className}`}
      >
        <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M3 9a2 2 0 012-2h.93a2 2 0 001.664-.89l.812-1.22A2 2 0 0110.07 4h3.86a2 2 0 011.664.89l.812 1.22A2 2 0 0018.07 7H19a2 2 0 012 2v9a2 2 0 01-2 2H5a2 2 0 01-2-2V9z"
          />
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M15 13a3 3 0 11-6 0 3 3 0 016 0z"
          />
        </svg>
        카드 촬영
      </Button>

      {/* 오류 메시지 표시 */}
      {errorMessage && (
        <div className="mt-2 p-2 text-red-500 bg-red-50 border border-red-100 rounded-md text-sm">
          {errorMessage}
        </div>
      )}

      {isActive && (
        <CameraModal
          videoRef={videoRef as React.RefObject<HTMLVideoElement>}
          onCapture={capturePhoto}
          onClose={handleCloseModal}
          onError={(msg) => {
            setErrorMessage(msg);
            onError?.(msg);
          }}
        />
      )}
    </div>
  );
};

export default CameraCapture;
