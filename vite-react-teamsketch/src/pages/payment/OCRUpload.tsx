import { useState } from "react";
import ImageUpload from "../../components/features/upload/ImageUpload";
import { fileUpload } from "../../services/api/cloudAPI";
import CardDetails from "./CardDetails";

interface OCRResult {
  status: string;
  data: {
    message: string;
    response: any;
  };
  code: string;
}

const OCRUpload = () => {
  const [ocrResult, setOcrResult] = useState<OCRResult | null>(null);
  const [cameraError, setCameraError] = useState<string | null>(null);

  const handleOCRUpload = async (formData: FormData) => {
    try {
      setCameraError(null);
      const result = await fileUpload.CloudOCR(formData);
      console.log("OCR 결과:", result);
      setOcrResult(result);
    } catch (error) {
      console.error("OCR 처리 중 오류:", error);
      setCameraError("OCR 처리 중 오류가 발생했습니다. 다시 시도하거나 직접 카드 정보를 입력해주세요.");
      setOcrResult(null);
    }
  };

  const handleCameraError = (message: string) => {
    console.error("카메라 오류:", message);
    setCameraError(message);
  };

  return (
    <div className="p-4">
      <h2 className="text-xl font-semibold mb-4">사진으로 카드 추가</h2>

      {/* 카메라 오류 메시지 */}
      {cameraError && (
        <div className="my-4 p-3 bg-red-50 border border-red-200 rounded-lg text-red-600 text-sm">
          <p className="font-medium mb-1">카메라/OCR 오류</p>
          <p>{cameraError}</p>
          <p className="mt-2 text-gray-600">갤러리에서 이미지를 선택하거나 아래 수동 입력 폼을 이용해주세요.</p>
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div>
          <h3 className="text-lg font-medium mb-3">1. 카메라로 카드 촬영하기</h3>
          <ImageUpload
            onUpload={handleOCRUpload}
            onError={handleCameraError}
            className="max-w-md mx-auto mb-4"
            type="ocr"
            borderStyle="border-2 border-dashed border-primary-500 rounded-lg dark:border-primary-500"
          />

          <h3 className="text-lg font-medium mb-3 mt-6">2. 갤러리에서 이미지 선택하기</h3>
          <ImageUpload
            onUpload={handleOCRUpload}
            onError={handleCameraError}
            className="max-w-md mx-auto"
            type="image"
            borderStyle="border-2 border-dashed border-primary-500 rounded-lg dark:border-primary-500"
          />
        </div>

        <div>
          {/* OCR 결과가 있을 때만 카드 상세 정보 표시 */}
          {ocrResult && <CardDetails ocrResult={ocrResult} />}

          {/* OCR 결과가 없을 때는 안내 메시지 표시 */}
          {!ocrResult && (
            <div className="bg-gray-50 p-4 rounded-lg border border-gray-200">
              <h3 className="text-lg font-medium mb-2">카드 정보 자동 인식</h3>
              <p className="text-gray-600 mb-4">
                카드를 촬영하거나 이미지를 업로드하면 자동으로 카드 정보를 인식합니다.
              </p>
              <p className="text-sm text-gray-500">
                인식이 어려운 경우 직접 카드 정보를 입력할 수 있습니다.
              </p>
            </div>
          )}
        </div>
      </div>

      {/* OCR 결과 디버깅 정보 (개발용) */}
      {ocrResult && process.env.NODE_ENV === 'development' && (
        <div className="mt-8 p-4 bg-gray-50 rounded-lg border border-gray-200">
          <h3 className="text-lg font-semibold mb-2">OCR 결과 (개발자용):</h3>
          <div className="whitespace-pre-wrap break-words">
            <p>상태: {ocrResult.status}</p>
            <p>메시지: {ocrResult.data.message}</p>
            <p>응답 데이터:</p>
            <pre className="bg-white p-2 rounded mt-2 overflow-x-auto">
              {JSON.stringify(ocrResult.data.response, null, 2)}
            </pre>
          </div>
        </div>
      )}
    </div>
  );
};

export default OCRUpload;
