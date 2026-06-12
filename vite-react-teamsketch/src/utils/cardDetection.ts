// Window 타입 확장
declare global {
  interface Window {
    cv: any;
    Module: any;
  }
}

// OpenCV 인스턴스가 이미 로드되었는지 확인하는 플래그
let isOpencvLoaded = false;

export const resetOpenCV = () => {
  // OpenCV 리셋 시 로드 상태도 초기화
  isOpencvLoaded = false;
  
  // 기존 스크립트 제거 (전역 객체 초기화)
  const existingScript = document.getElementById('opencv-script');
  if (existingScript) {
    existingScript.remove();
    console.log('기존 OpenCV 스크립트 제거됨');
    
    // cv 객체 언마운트 (필요한 경우)
    if (window.cv) {
      // 안전하게 삭제
      try {
        delete window.cv;
        delete window.Module;
      } catch (e) {
        console.error('OpenCV 객체 삭제 실패:', e);
        // 직접 null 할당으로 대체
        (window as any).cv = null;
        (window as any).Module = null;
      }
    }
  }
  
  // 메모리 정리 요청
  if (typeof global !== 'undefined' && global.gc) {
    try {
      global.gc();
    } catch (error: unknown) {
      const errorMessage = error instanceof Error ? error.message : '알 수 없는 오류';
      console.log(`가비지 컬렉션 요청 실패: ${errorMessage}`);
    }
  }
};

export const loadOpenCV = async (onProgress?: (progress: number) => void): Promise<boolean> => {
  return new Promise((resolve) => {
    // 이미 로드되었는지 확인
    if (isOpencvLoaded && window.cv) {
      console.log('OpenCV.js가 이미 로드되어 있습니다.');
      if (onProgress) onProgress(100);
      resolve(true);
      return;
    }

    // 타임아웃 설정 (20초로 증가 - 대용량 파일 로딩 고려)
    const timeoutDuration = 20000;
    const timeoutId = setTimeout(() => {
      console.error('OpenCV.js 로딩 타임아웃. 20초 초과');
      if (onProgress) onProgress(0);
      isOpencvLoaded = false;
      resolve(false);
    }, timeoutDuration);

    // 이미 스크립트가 로드 중인지 확인
    const existingScript = document.getElementById('opencv-script');
    if (existingScript) {
      console.log('OpenCV.js 스크립트가 이미 로드 중입니다.');
      clearTimeout(timeoutId);
      resolve(true);
      return;
    }

    console.log('OpenCV.js 로딩 시작...');
    if (onProgress) onProgress(10);

    // OpenCV.js 스크립트 생성
    const script = document.createElement('script');
    script.id = 'opencv-script';
    script.async = true;
    script.type = 'text/javascript';
    
    // CDN URL 사용 (만약 로컬 파일이 있다면 그것을 우선 사용)
    script.src = '/lib/opencv.js';

    // 로딩 완료 처리
    script.onload = () => {
      console.log('OpenCV.js 스크립트 로드 완료. 초기화 대기 중...');
      if (onProgress) onProgress(50);

      // IntVector 바인딩 오류 방지를 위한 처리
      try {
        // 전역 오류 핸들러 등록
        const originalErrorHandler = window.onerror;
        window.onerror = (message, source, lineno, colno, error) => {
          if (message.toString().includes('IntVector') || 
              (error && error.message && error.message.includes('IntVector'))) {
            console.warn('IntVector 바인딩 오류 감지됨. 무시하고 계속 진행합니다.');
            // true를 반환하여 오류 전파 차단
            return true;
          }
          // 원래 오류 핸들러 호출
          if (originalErrorHandler) return originalErrorHandler(message, source, lineno, colno, error);
          return false;
        };
      } catch (error) {
        console.warn('오류 핸들러 등록 실패:', error);
      }

      if (window.cv) {
        console.log('OpenCV.js가 사전 초기화되어 있습니다.');
        if (onProgress) onProgress(100);
        isOpencvLoaded = true;
        clearTimeout(timeoutId);
        resolve(true);
        return;
      }

      // OpenCV 런타임 초기화 완료 대기
      window.Module = {
        onRuntimeInitialized: () => {
          console.log('OpenCV.js 초기화 완료!');
          if (onProgress) onProgress(100);
          isOpencvLoaded = true;
          clearTimeout(timeoutId);
          resolve(true);
        },
        setStatus: (status: string) => {
          // OpenCV 로딩 상태 처리
          console.log('OpenCV 상태:', status);
          
          // 로딩 진행률 추출 시도
          try {
            if (status.includes('Downloading')) {
              // 다운로드 진행률 파싱 (예: "Downloading data... (50%)")
              const progressMatch = status.match(/\((\d+)%\)/);
              if (progressMatch && progressMatch[1]) {
                const downloadProgress = parseInt(progressMatch[1], 10);
                // 다운로드는 50%까지만 진행률 표시 (50% 이후는 초기화 과정)
                const actualProgress = 50 + (downloadProgress / 2);
                if (onProgress) onProgress(actualProgress);
                console.log(`OpenCV 로딩 진행률: ${actualProgress}%`);
              }
            } else if (status.includes('Running')) {
              // 초기화 중
              if (onProgress) onProgress(90);
              console.log('OpenCV 초기화 중...');
            }
          } catch (error) {
            console.warn('OpenCV 로딩 상태 처리 중 오류:', error);
          }
        },
        setProgress: (progress: number) => {
          // 진행률 정보가 제공되는 경우
          try {
            if (onProgress && progress >= 0 && progress <= 1) {
              // 0-1 사이의 값을 50-90% 범위로 변환 (초기 50%는 스크립트 로드)
              const actualProgress = 50 + Math.round(progress * 40);
              onProgress(actualProgress);
              console.log(`OpenCV 로딩 진행률: ${actualProgress}%`);
            }
          } catch (error) {
            console.warn('OpenCV 진행률 처리 중 오류:', error);
          }
        }
      };
    };

    // 로딩 실패 처리
    script.onerror = () => {
      console.error('OpenCV.js 스크립트 로드 실패');
      if (onProgress) onProgress(0);
      clearTimeout(timeoutId);
      resolve(false);
    };

    // 스크립트 추가
    document.body.appendChild(script);
    console.log('OpenCV.js 스크립트 추가됨');
    if (onProgress) onProgress(20);
  });
};

// 카드 비율 상수 (신용카드 표준 비율)
const CARD_RATIO = 85.6 / 53.98; // 가로 / 세로 비율 (약 1.586)

/**
 * 카드 감지 함수
 * @param video 비디오 엘리먼트
 * @param canvas 캔버스 엘리먼트
 * @returns 카드가 감지되었는지 여부와 상태 메시지
 */
export async function detectCard(
  video: HTMLVideoElement, 
  canvas: HTMLCanvasElement
): Promise<{ isDetected: boolean; status: string }> {
  // 이 함수에서 사용할 OpenCV 객체들 선언
  let src: any = null;
  let gray: any = null;
  let blurred: any = null;
  let edges: any = null;
  let contours: any = null;
  let hierarchy: any = null;
  let cnt: any = null;
  let approx: any = null;

  try {
    // OpenCV가 초기화되지 않은 경우 로드 시도
    if (!isOpencvLoaded || !window.cv) {
      console.log('OpenCV 초기화되지 않음, 로딩 시도...');
      await loadOpenCV();
    }

    // OpenCV 객체 확인
    if (!window.cv) {
      return { isDetected: false, status: 'OpenCV 라이브러리를 사용할 수 없습니다' };
    }

    const ctx = canvas.getContext('2d');
    if (!ctx) {
      console.error('캔버스 컨텍스트를 가져올 수 없습니다.');
      return { isDetected: false, status: '캔버스 초기화 실패' };
    }

    // 비디오 상태 확인
    if (video.videoWidth === 0 || video.videoHeight === 0) {
      return { isDetected: false, status: '비디오 스트림이 준비되지 않았습니다' };
    }

    // 캔버스 크기 설정
    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;

    // 비디오 프레임을 캔버스에 그리기
    ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
    
    // OpenCV.js 객체 생성
    src = window.cv.imread(canvas);
    gray = new window.cv.Mat();
    blurred = new window.cv.Mat();
    edges = new window.cv.Mat();
    contours = new window.cv.MatVector();
    hierarchy = new window.cv.Mat();

    // 이미지 전처리
    window.cv.cvtColor(src, gray, window.cv.COLOR_RGBA2GRAY, 0);
    window.cv.GaussianBlur(gray, blurred, new window.cv.Size(5, 5), 0, 0, window.cv.BORDER_DEFAULT);
    window.cv.Canny(blurred, edges, 75, 150);

    // 윤곽선 찾기
    window.cv.findContours(edges, contours, hierarchy, window.cv.RETR_EXTERNAL, window.cv.CHAIN_APPROX_SIMPLE);

    // 윤곽선 분석
    let cardDetected = false;
    let largestArea = 0;
    let bestContourIndex = -1;

    // 먼저 가장 큰 윤곽선을 찾습니다 (작은 윤곽선은 무시)
    for (let i = 0; i < contours.size(); i++) {
      const contour = contours.get(i);
      const area = window.cv.contourArea(contour);
      
      // 최소 크기 필터링 - 너무 작은 윤곽선은 무시
      const minArea = (canvas.width * canvas.height) * 0.03; // 화면의 3% 이상
      
      if (area > minArea && area > largestArea) {
        largestArea = area;
        bestContourIndex = i;
      }
      
      contour.delete();
    }
    
    // 가장 큰 윤곽선이 카드 형태인지 확인
    if (bestContourIndex >= 0) {
      cnt = contours.get(bestContourIndex);
      approx = new window.cv.Mat();
      
      // 윤곽선 근사화
      window.cv.approxPolyDP(cnt, approx, 0.02 * window.cv.arcLength(cnt, true), true);
      
      // 4개의 꼭지점을 가진 윤곽선 확인 (카드 모양)
      if (approx.rows === 4) {
        const rect = window.cv.boundingRect(approx);
        const ratio = rect.width / rect.height;
        
        // 카드 비율과 유사한지 확인 (10% 오차 허용)
        const ratioError = Math.abs(ratio - CARD_RATIO) / CARD_RATIO;
        if (ratioError < 0.1) {
          cardDetected = true;
          
          // 시각적 피드백을 위해 감지된 윤곽선 그리기 (개발 모드에서만)
          if (process.env.NODE_ENV === 'development') {
            const color = new window.cv.Scalar(0, 255, 0, 255); // 녹색
            const thickness = 3;
            window.cv.drawContours(src, contours, bestContourIndex, color, thickness);
            window.cv.imshow(canvas, src);
          }
        }
      }
    }

    return { 
      isDetected: cardDetected, 
      status: cardDetected ? '카드가 감지되었습니다!' : '카드를 찾을 수 없습니다' 
    };
  } catch (error) {
    console.error('카드 감지 중 오류 발생:', error);
    return { isDetected: false, status: '감지 중 오류가 발생했습니다' };
  } finally {
    // 모든 OpenCV 자원 해제
    try {
      if (src) src.delete();
      if (gray) gray.delete();
      if (blurred) blurred.delete();
      if (edges) edges.delete();
      if (contours) contours.delete();
      if (hierarchy) hierarchy.delete();
      if (approx) approx.delete();
      if (cnt) cnt.delete();
    } catch (cleanupError) {
      console.error('OpenCV 자원 정리 중 오류:', cleanupError);
    }
  }
}