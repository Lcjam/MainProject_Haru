import { useEffect, useRef, useState } from 'react';
import { loadOpenCV, detectCard, resetOpenCV } from '../../../utils/cardDetection';

interface UseCardScannerParams {
  videoRef: React.RefObject<HTMLVideoElement | null>;
  onCapture: () => void;
  onClose: () => void;
  onError: (message: string) => void;
}

interface UseCardScannerResult {
  loading: boolean;
  loadingProgress: number;
  cardDetected: boolean;
  detectionStatus: string;
  canvasRef: React.RefObject<HTMLCanvasElement | null>;
  handleCapture: () => void;
  handleClose: () => void;
}

const AUTO_CAPTURE_DURATION = 2000; // 자동 촬영을 위한 카드 감지 유지 시간 (2초)

/**
 * 카메라 스트림 + OpenCV 카드 감지/자동 촬영 로직.
 * CameraModal에서 분리해 UI(프레젠테이션)와 디바이스/CV 로직을 분리한다.
 */
export const useCardScanner = ({
  videoRef,
  onCapture,
  onClose,
  onError
}: UseCardScannerParams): UseCardScannerResult => {
  const [loading, setLoading] = useState(true);
  const [opencvLoaded, setOpencvLoaded] = useState(false);
  const [loadingProgress, setLoadingProgress] = useState(0);
  const [cardDetected, setCardDetected] = useState(false);
  const [detectionStatus, setDetectionStatus] = useState('');
  const [cardStableTime, setCardStableTime] = useState<number | null>(null); // 카드 감지 시작 시간
  const [autoCapturing, setAutoCapturing] = useState(false); // 자동 촬영 중인지 여부

  const canvasRef = useRef<HTMLCanvasElement>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const captureEnabled = useRef<boolean>(true);
  const autoCaptureCooldown = useRef<boolean>(false); // 자동 촬영 쿨다운

  const handleCapture = () => {
    if (!videoRef.current) return;

    try {
      onCapture();
      // 촬영 후 항상 모달을 닫음
      handleClose();
    } catch (error) {
      console.error('촬영 오류:', error);
      onError('이미지 촬영 중 오류가 발생했습니다.');
      // 오류 발생 시에도 모달 닫기
      handleClose();
    }
  };

  const handleClose = () => {
    // OpenCV 리셋
    resetOpenCV();
    setOpencvLoaded(false);

    // 모달 닫기
    onClose();
  };

  // OpenCV 로드 및 카메라 초기화
  useEffect(() => {
    let isMounted = true;

    const initCamera = async () => {
      try {
        setLoading(true);

        // 이미 스트림이 있다면 정리
        if (streamRef.current) {
          streamRef.current.getTracks().forEach((track) => track.stop());
          streamRef.current = null;
        }

        // OpenCV 로드
        const loaded = await loadOpenCV((progress) => {
          setLoadingProgress(progress);
        });

        if (!loaded) {
          console.error('OpenCV.js 로드 실패');
          onError('OpenCV를 로드할 수 없습니다. 잠시 후 다시 시도해주세요.');
          return;
        }

        setOpencvLoaded(true);

        // 카메라 스트림 가져오기
        const constraints = {
          video: {
            width: { ideal: 1920 },
            height: { ideal: 1080 },
            facingMode: 'environment',
            aspectRatio: { ideal: 16 / 9 } // 16:9 비율로 요청
          }
        };

        try {
          const stream = await navigator.mediaDevices.getUserMedia(constraints);

          if (!isMounted) {
            // 컴포넌트가 언마운트되었다면 스트림 정리
            stream.getTracks().forEach((track) => track.stop());
            return;
          }

          streamRef.current = stream;

          if (videoRef.current) {
            videoRef.current.srcObject = stream;
            videoRef.current
              .play()
              .then(() => {
                setLoading(false);
                captureEnabled.current = true;
              })
              .catch((err) => {
                console.error('비디오 재생 실패:', err);
                onError('카메라 스트림을 재생할 수 없습니다. 브라우저 설정을 확인해주세요.');
                setLoading(false);
              });
          } else {
            console.error('비디오 요소를 찾을 수 없음');
            onError('카메라를 초기화할 수 없습니다. 페이지를 새로고침해 주세요.');
            setLoading(false);
          }
        } catch (streamError) {
          if (!isMounted) return;
          console.error('카메라 스트림 획득 실패:', streamError);
          onError('카메라에 접근할 수 없습니다. 카메라 권한을 확인해주세요.');
          setLoading(false);
        }
      } catch (error) {
        if (!isMounted) return;
        console.error('카메라 초기화 오류:', error);
        const errorMessage =
          error instanceof Error ? error.message : '카메라를 초기화하는 중 오류가 발생했습니다.';
        onError(errorMessage);
        setLoading(false);
      }
    };

    initCamera();

    // 컴포넌트 언마운트 시 정리
    return () => {
      isMounted = false;
      // 카메라 스트림 정리
      if (streamRef.current) {
        streamRef.current.getTracks().forEach((track) => {
          track.stop();
        });
        streamRef.current = null;
      }

      // 비디오 요소 정리
      if (videoRef.current) {
        videoRef.current.srcObject = null;
      }

      // OpenCV 리셋
      resetOpenCV();
    };
  }, [videoRef, onError]);

  // 카드 감지 로직
  useEffect(() => {
    let animationFrameId: number;
    let lastDetectionTime = 0;
    const DETECTION_INTERVAL = 200; // 0.2초마다 감지 실행(더 빠르게)
    let lastVibrationTime = 0;
    const VIBRATION_INTERVAL = 1000; // 진동 간격(1초)
    let errorCount = 0; // 연속 오류 카운트

    const detectCardInFrame = async () => {
      if (!videoRef.current || !canvasRef.current || !opencvLoaded || loading) {
        animationFrameId = requestAnimationFrame(detectCardInFrame);
        return;
      }

      const now = Date.now();
      if (now - lastDetectionTime > DETECTION_INTERVAL) {
        lastDetectionTime = now;

        try {
          // 카드 감지 실행
          const result = await detectCard(videoRef.current, canvasRef.current);

          // 오류 발생 후 성공적으로 감지 재개된 경우
          errorCount = 0;

          // 카드 감지 상태가 변경되었을 때만 상태 업데이트
          if (result.isDetected !== cardDetected) {
            setCardDetected(result.isDetected);
            setDetectionStatus(result.status);

            // 카드가 감지되었을 때
            if (result.isDetected) {
              // 카드 감지 시작 시간 설정 (아직 설정되지 않은 경우)
              if (cardStableTime === null) {
                setCardStableTime(now);
              }

              // 진동 피드백 (1초에 한 번만)
              if (now - lastVibrationTime > VIBRATION_INTERVAL && navigator.vibrate) {
                navigator.vibrate(100);
                lastVibrationTime = now;
              }
            } else {
              // 카드 감지가 중단되면 시작 시간 초기화
              setCardStableTime(null);
            }
          }

          // 자동 촬영 로직: 카드가 감지된 상태가 AUTO_CAPTURE_DURATION 이상 유지되면 자동 촬영
          if (
            result.isDetected &&
            cardStableTime !== null &&
            !autoCapturing &&
            !autoCaptureCooldown.current
          ) {
            const detectionDuration = now - cardStableTime;

            // 진행 상황 표시 (0%~100%)
            const captureProgress = Math.min(
              100,
              Math.round((detectionDuration / AUTO_CAPTURE_DURATION) * 100)
            );
            setDetectionStatus(`카드 감지 완료: ${captureProgress}%`);

            // 설정된 시간 이상 카드가 감지되면 자동 촬영
            if (detectionDuration >= AUTO_CAPTURE_DURATION) {
              // 중복 촬영 방지를 위한 플래그 설정
              setAutoCapturing(true);
              autoCaptureCooldown.current = true;

              // 촬영 시작 알림
              setDetectionStatus('자동 촬영 중...');

              // 진동으로 촬영 알림
              if (navigator.vibrate) {
                navigator.vibrate([100, 100, 200]);
              }

              try {
                // 약간의 지연 후 촬영 (사용자가 인지할 수 있도록)
                setTimeout(() => {
                  try {
                    handleCapture(); // 이 함수가 내부적으로 모달을 닫음
                  } catch (error) {
                    console.error('자동 촬영 오류:', error);
                    setAutoCapturing(false);
                    autoCaptureCooldown.current = false;
                    setCardStableTime(null);
                    onError('카드 촬영 중 오류가 발생했습니다. 다시 시도해주세요.');
                    // 오류 발생 시에도 모달 닫기
                    handleClose();
                  }
                }, 500);
              } catch (error) {
                console.error('자동 촬영 타이머 오류:', error);
                setAutoCapturing(false);
                autoCaptureCooldown.current = false;
                setCardStableTime(null);
                // 오류 발생 시에도 모달 닫기
                handleClose();
              }
            }
          }
        } catch (error) {
          console.error('카드 감지 오류:', error);

          // 연속 오류 카운트 증가
          errorCount++;

          // 'IntVector' 바인딩 오류 즉시 처리
          if (
            error instanceof Error &&
            (error.name === 'BindingError' ||
              error.message.includes('IntVector') ||
              error.message.includes('register'))
          ) {
            // 바인딩 오류는 즉시 모달 닫기
            setDetectionStatus('OpenCV 오류 발생, 모달을 닫습니다');
            onError(`OpenCV 오류가 발생했습니다. 촬영된 이미지를 사용하실 수 있습니다.`);
            handleClose(); // 모달 닫기
            return;
          } else if (errorCount >= 5) {
            // 5번 이상 연속으로 다른 오류가 발생한 경우
            setDetectionStatus('카메라 처리 오류 발생');
            onError('카메라 처리 중 오류가 발생했습니다. 다시 시도해주세요.');
          }
        }
      }

      // 다음 프레임 요청
      animationFrameId = requestAnimationFrame(detectCardInFrame);
    };

    // 카드 감지 시작
    animationFrameId = requestAnimationFrame(detectCardInFrame);

    // 컴포넌트 언마운트 시 정리
    return () => {
      cancelAnimationFrame(animationFrameId);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [cardDetected, videoRef, opencvLoaded, loading, cardStableTime, autoCapturing, onError]);

  // 키보드 Esc 키 이벤트 처리
  useEffect(() => {
    const handleEscKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        handleClose();
      }
    };

    document.addEventListener('keydown', handleEscKey);
    return () => {
      document.removeEventListener('keydown', handleEscKey);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return {
    loading,
    loadingProgress,
    cardDetected,
    detectionStatus,
    canvasRef,
    handleCapture,
    handleClose
  };
};
