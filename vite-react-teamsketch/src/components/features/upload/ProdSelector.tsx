import React from 'react';

interface ImageSelectorProps {
  onFileSelect: (file: File) => void;
  className?: string;
  text?: string;
  multiple?: boolean;
}

const ProdSelector: React.FC<ImageSelectorProps> = ({
  onFileSelect,
  className = '',
  text = '상품 사진 등록',
  multiple = false
}) => {
  const handleFileChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const files = event.target.files;
    if (!files || files.length === 0) return;

    // 여러 파일 처리
    if (multiple && files.length > 1) {
      Array.from(files).forEach((file) => {
        if (file.size > 5 * 1024 * 1024) {
          alert(`파일 ${file.name}의 크기는 5MB 이하여야 합니다.`);
          return;
        }
        onFileSelect(file);
      });
    } else {
      // 단일 파일 처리
      const file = files[0];
      if (file.size > 5 * 1024 * 1024) {
        alert('파일 크기는 5MB 이하여야 합니다.');
        return;
      }
      onFileSelect(file);
    }
  };

  return (
    <div className={className}>
      <input
        type="file"
        accept="image/*"
        onChange={handleFileChange}
        className="hidden"
        id="prod-upload"
        multiple={multiple}
      />
      <label
        htmlFor="prod-upload"
        className="cursor-pointer flex items-center justify-center px-4 py-2 border-2 border-dashed border-primary rounded-lg hover:bg-primary-lightest transition-colors"
      >
        {text}
      </label>
    </div>
  );
};

export default ProdSelector;
