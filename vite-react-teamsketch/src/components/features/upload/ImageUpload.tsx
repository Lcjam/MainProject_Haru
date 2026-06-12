import React, { useState } from 'react';
import Button from '../../common/Button';
import ImageSelector from './ImageSelector';
import CameraCapture from './CameraCapture';
import ProfileSelector from './ProfileSelector';
import ProdSelector from './ProdSelector';
import { useDispatch } from 'react-redux';
import { updateProfileImage } from '../../../store/slices/userSlice';

// test
interface ImageUploadProps {
  onUpload?: (formData: FormData) => Promise<any>;
  onFileSelect?: (file: File) => void;
  className?: string;
  type?: ImageUploadType;
  isEdit?: boolean;
  currentImage?: File | string;
  imageUrl?: string;
  multiple?: boolean;
  images?: File[];
  onRemove?: (index: number) => void;
  maxImages?: number;
  borderStyle?: string;
  onError?: (message: string) => void;
}

type ImageUploadType = 'ocr' | 'image' | 'profile' | 'prod';

const ImageUpload: React.FC<ImageUploadProps> = ({
  onUpload = async () => {},
  onFileSelect,
  className = '',
  type = 'ocr',
  currentImage,
  isEdit = false,
  multiple = false,
  images = [],
  onRemove,
  maxImages = 10,
  borderStyle = '',
  onError
}) => {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const dispatch = useDispatch();

  const handleFileSelect = (file: File) => {
    if (!multiple) {
      setSelectedFile(file);
      setPreviewUrl(URL.createObjectURL(file));
      return;
    }

    // 다중 이미지 업로드 처리
    if (images.length >= maxImages) {
      alert(`최대 ${maxImages}개의 이미지만 업로드할 수 있습니다.`);
      return;
    }

    // 이미지가 이미 존재하는지 확인 (중복 체크 강화)
    const isDuplicate = images.some((img) => img.name === file.name && img.size === file.size);

    if (isDuplicate) {
      console.log('중복 이미지 감지됨 (ImageUpload):', file.name);
      alert('이미 추가된 이미지입니다.');
      return;
    }

    console.log('이미지 추가 (ImageUpload):', file.name);
    onFileSelect?.(file);
  };

  const handleUpload = async () => {
    if (!selectedFile) return;

    setIsLoading(true);
    const formData = new FormData();
    formData.append('file', selectedFile);

    try {
      await onUpload(formData);
      if (type === 'profile') {
        dispatch(updateProfileImage(selectedFile));
      }
      setSelectedFile(null);
      setPreviewUrl(null);
    } catch (error) {
      console.error('업로드 실패:', error);
      onError?.('파일 업로드에 실패했습니다. 다시 시도해주세요.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className={`space-y-4 ${className}`}>
      <div className="flex flex-col items-center gap-4">
        <div className={`flex gap-4 ${borderStyle}`}>
          {type === 'ocr' && (
            <CameraCapture 
              onCapture={handleFileSelect} 
              className="text-primary-500"
              onError={onError}
            />
          )}
          {type === 'image' && (
            <ImageSelector 
              onFileSelect={handleFileSelect} 
              className="text-primary-500"
              onError={onError}
            />
          )}
          {type === 'prod' && (
            <ProdSelector
              onFileSelect={handleFileSelect}
              className="text-primary-500"
              multiple={multiple}
            />
          )}
          {type === 'profile' && (
            <ProfileSelector
              onFileSelect={handleFileSelect}
              file={currentImage as File | string}
              isEditable={isEdit}
              imageUrl={previewUrl || undefined}
            />
          )}
        </div>

        {/* 다중 이미지 미리보기 */}
        {multiple && images.length > 0 && (
          <div className="w-full max-w-full">
            <div className="relative">
              <div className="overflow-x-auto scrollbar-hide" style={{ maxWidth: '100%' }}>
                <div className="flex gap-4 pb-4 pt-4">
                  {images.map((image, index) => (
                    <div
                      key={index}
                      className="relative group flex-shrink-0"
                      style={{ width: '200px', height: '200px' }}
                    >
                      <div className="w-full h-full rounded-lg overflow-hidden border border-gray-200">
                        <img
                          src={URL.createObjectURL(image)}
                          alt={`이미지 ${index + 1}`}
                          className="w-full h-full object-cover"
                        />
                      </div>
                      {onRemove && (
                        <button
                          type="button"
                          onClick={() => onRemove(index)}
                          className="absolute -top-2 -right-2 bg-red-500 hover:bg-red-600 text-white rounded-full w-6 h-6 flex items-center justify-center shadow-md transition-colors z-10"
                          aria-label="이미지 삭제"
                        >
                          ×
                        </button>
                      )}
                      <div className="absolute inset-0 bg-black bg-opacity-0 group-hover:bg-opacity-20 transition-opacity rounded-lg" />
                      <div className="absolute bottom-2 left-2 bg-black bg-opacity-50 text-white px-2 py-1 rounded-md text-sm">
                        {index + 1}/{images.length}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
              {images.length > 3 && (
                <div className="absolute bottom-0 left-0 right-0 h-1 bg-gray-100 rounded">
                  <div
                    className="h-full bg-primary rounded"
                    style={{
                      width: `${(3 / images.length) * 100}%`,
                      minWidth: '20%'
                    }}
                  />
                </div>
              )}
            </div>
            <p className="text-xs text-gray-500 mt-2 text-center">
              ← 좌우로 스크롤하여 더 많은 이미지를 확인할 수 있습니다 →
            </p>
          </div>
        )}

        {/* 단일 이미지 미리보기 */}
        {!multiple && previewUrl && (
          <div className="relative">
            <img src={previewUrl} alt="미리보기" className="max-w-xs rounded-lg shadow-md" />
            <button
              onClick={() => {
                setSelectedFile(null);
                setPreviewUrl(null);
              }}
              className="absolute top-2 right-2 p-1 bg-red-500 text-white rounded-full hover:bg-red-600"
            >
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M6 18L18 6M6 6l12 12"
                />
              </svg>
            </button>
          </div>
        )}

        {/* 단일 이미지 업로드 버튼 */}
        {!multiple && selectedFile && (
          <Button className="bg-primary-400" onClick={handleUpload} disabled={isLoading}>
            {isLoading ? '업로드 중...' : '업로드'}
          </Button>
        )}

        {/* 다중 이미지 업로드 상태 표시 */}
        {multiple && (
          <div className="text-sm text-gray-500">
            {images.length}/{maxImages}개의 이미지
          </div>
        )}
      </div>
    </div>
  );
};

export default ImageUpload;
