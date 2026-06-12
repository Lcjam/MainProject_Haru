import { memo } from 'react';
import { useProductImage, extractImageIdFromPath } from '../../../services/api/productAPI';
import defaultImage1 from '../../../assets/images/default/product-image-1.png';
import defaultImage2 from '../../../assets/images/default/product-image-2.png';
import defaultImage3 from '../../../assets/images/default/product-image-3.png';
import defaultImage4 from '../../../assets/images/default/product-image-4.png';
import defaultImage5 from '../../../assets/images/default/product-image-5.png';

const ProductImage = memo(({ imagePath }: { imagePath: string }) => {
  const imageId = imagePath ? extractImageIdFromPath(imagePath) : null;
  const { data: imageBlob, isLoading } = useProductImage(imageId || 0);

  // 이미지 경로가 없거나 로딩 중일 때 기본 이미지 표시
  if (!imagePath || isLoading) {
    // 기본 이미지 배열
    const defaultImages = [
      defaultImage1,
      defaultImage2,
      defaultImage3,
      defaultImage4,
      defaultImage5
    ];
    
    // 랜덤하게 기본 이미지 선택
    const randomImage = defaultImages[Math.floor(Math.random() * defaultImages.length)];
    return <img src={randomImage} alt="기본 이미지" className="w-full h-full object-cover" />;
  }

  // 이미지 로드 실패 시 대체 이미지 표시
  if (!imageBlob) {
    // 이미지 ID를 사용하여 고유한 랜덤 이미지 생성
    const fallbackImageUrl = `https://picsum.photos/600/400?random=${
      imageId || Math.floor(Math.random() * 1000)
    }`;
    return <img src={fallbackImageUrl} alt="상품 이미지" className="w-full h-full object-cover" />;
  }

  return (
    <img
      src={URL.createObjectURL(imageBlob)}
      alt="상품 이미지"
      className="w-full h-full object-cover"
    />
  );
});

export default ProductImage;
