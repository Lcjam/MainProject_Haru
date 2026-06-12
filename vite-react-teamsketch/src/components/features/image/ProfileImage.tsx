import { memo, useMemo } from 'react';
import { useUserProfileImage } from '../../../services/api/profileImageAPI';  
import defaultImage1 from '../../../assets/images/default/product-image-1.png';
import defaultImage2 from '../../../assets/images/default/product-image-2.png';
import defaultImage3 from '../../../assets/images/default/product-image-3.png';
import defaultImage4 from '../../../assets/images/default/product-image-4.png';
import defaultImage5 from '../../../assets/images/default/product-image-5.png';

interface ProfileImageProps {
  imagePath?: string;
  className?: string;
  nickname?: string;
}

const ProfileImage = memo(({ imagePath, className = "w-full h-full object-cover", nickname }: ProfileImageProps) => {
  // imagePath가 있으면 직접 사용, 없으면 nickname으로 프로필 이미지 조회
  const { data: profileImage, isLoading } = useUserProfileImage(nickname || '');

  // 이미지 URL 생성
  const imageUrl = useMemo(() => {
    if (imagePath && imagePath.startsWith('blob:')) {
      return imagePath;
    }

    if (profileImage?.status === 'success' && profileImage.data.response) {
      return URL.createObjectURL(profileImage.data.response);
    }

    // 기본 이미지 배열
    const defaultImages = [
      defaultImage1,
      defaultImage2,
      defaultImage3,
      defaultImage4,
      defaultImage5
    ];
    
    // 랜덤하게 기본 이미지 선택 (nickname이 있으면 그것을 기준으로, 없으면 완전 랜덤)
    const index = nickname 
      ? nickname.split('').reduce((acc, char) => acc + char.charCodeAt(0), 0) % defaultImages.length
      : Math.floor(Math.random() * defaultImages.length);
    
    return defaultImages[index];
  }, [imagePath, profileImage, nickname]);

  // 컴포넌트 언마운트 시 URL 정리
  useMemo(() => {
    return () => {
      if (imageUrl && imageUrl.startsWith('blob:')) {
        URL.revokeObjectURL(imageUrl);
      }
    };
  }, [imageUrl]);

  if (isLoading) {
    return (
      <div className={`${className} bg-gray-200 dark:bg-gray-700 animate-pulse`}>
        <div className="w-full h-full" />
      </div>
    );
  }

  return (
    <img
      src={imageUrl}
      alt={`${nickname || '사용자'} 프로필`}
      className={className}
      onError={(e) => {
        const target = e.target as HTMLImageElement;
        // 에러 시 기본 이미지로 대체
        target.src = defaultImage1;
      }}
    />
  );
});

ProfileImage.displayName = 'ProfileImage';

export default ProfileImage;
