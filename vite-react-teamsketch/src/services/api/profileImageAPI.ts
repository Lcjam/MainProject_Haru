import { axiosInstance, uploadInstance } from './axiosInstance';
import { apiConfig } from './apiConfig';
import { FileResponse } from '../../types/fileResponse';
import { useQuery } from '@tanstack/react-query';

const getImageFilename = (imageUrl: string): string | null => {
  if (!imageUrl) return null; // URL이 없을 경우 예외 처리

  const parts = imageUrl.split("/profile-images/");
  return parts.length > 1 ? parts[1] : null; // 파일명이 존재하면 반환
};

export const getProfileImage = async (): Promise<FileResponse | null> => {
  const token = localStorage.getItem('token');
  if (!token) {
    console.error('토큰이 없습니다.');
    return null;
  }
  console.log('getProfileImage 함수 호출됨');

  try {
    console.log('이미지 정보 요청 URL:', apiConfig.endpoints.core.getProfileImageInfo);
    const infoResponse = await axiosInstance.get(apiConfig.endpoints.core.getProfileImageInfo);

    console.log('이미지 정보 응답:', infoResponse.data.data.imageUrl);

    if (infoResponse.data?.data?.imageUrl) {
      const imageUrl = infoResponse.data.data.imageUrl;
      const filename = getImageFilename(imageUrl);
      console.log('imageUrl', imageUrl);
      console.log('이미지 파일명:', filename);

      if (!filename) {
        console.error('유효한 이미지 파일명을 추출할 수 없습니다.');
        return null;
      }

      try {
        console.log('이미지 파일 경로:', `${apiConfig.endpoints.core.getProfileImage}/${filename}`);
        const imageResponse = await axiosInstance.get(
          `${apiConfig.endpoints.core.getProfileImage}/${filename}`,
          {
            responseType: 'arraybuffer'
          }
        );

        const contentType = imageResponse.headers['content-type'] || 'image/jpeg';

        const blob = new Blob([imageResponse.data], { type: contentType });

        const imageFile = new File([blob], filename, { type: contentType });

        return {
          status: 'success',
          data: {
            message: filename,
            response: imageFile
          },
          code: '200'
        };
      } catch (error) {
        console.error('이미지 파일 조회 실패:', error);
        return null;
      }
    }

    return null;
  } catch (error) {
    console.error('프로필 이미지 조회 에러:', error);
    console.error('에러 상세:', {
      message: error instanceof Error ? error.message : '알 수 없는 에러',
      status: (error as any).response?.status,
      data: (error as any).response?.data
    });
    return null;
  }
};

export const coreProfile = async (formData: FormData): Promise<FileResponse> => {
  try {
    const response = await uploadInstance.post(apiConfig.endpoints.core.uploadProfile, formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    });
    console.log(response.data);
    if (response.data && typeof response.data === 'object') {
      return {
        status: 'success',
        data: response.data.data,
        code: response.data.code
      };
    }

    throw new Error('Invalid response format');
  } catch (error) {
    console.error('Profile API Error:', error);
    return {
      status: 'error',
      data: {
        message: error instanceof Error ? error.message : '이미지 처리 중 오류가 발생했습니다.',
        response: null
      },
      code: '500'
    };
  }
};

/**
 * 닉네임으로 사용자의 프로필 이미지 조회
 */
export const getUserProfileImageByNickname = async (nickname: string): Promise<FileResponse | null> => {
  try {
    // 1. 먼저 닉네임으로 프로필 정보 조회
    const profileResponse = await axiosInstance.get(
      `${apiConfig.endpoints.core.getUserProfileImage(nickname)}`
    );

    if (profileResponse.data?.status === 'success' && profileResponse.data?.data?.imageUrl) {
      const imageUrl = profileResponse.data.data.imageUrl;
      const filename = getImageFilename(imageUrl);
      
      if (!filename) {
        console.error('유효한 이미지 파일명을 추출할 수 없습니다.');
        return null;
      }

      // 2. 이미지 파일 조회
      try {
        const imageResponse = await axiosInstance.get(
          `${apiConfig.endpoints.core.getProfileImage}/${filename}`,
          {
            responseType: 'arraybuffer'
          }
        );

        const contentType = imageResponse.headers['content-type'] || 'image/jpeg';
        const blob = new Blob([imageResponse.data], { type: contentType });
        const imageFile = new File([blob], filename, { type: contentType });

        return {
          status: 'success',
          data: {
            message: filename,
            response: imageFile
          },
          code: '200'
        };
      } catch (error) {
        console.error('프로필 이미지 파일 조회 실패:', error);
        return null;
      }
    }

    return null;
  } catch (error) {
    console.error('사용자 프로필 조회 에러:', error);
    console.error('에러 상세:', {
      message: error instanceof Error ? error.message : '알 수 없는 에러',
      status: (error as any).response?.status,
      data: (error as any).response?.data
    });
    return null;
  }
};

// React Query Hook 추가
export const useUserProfileImage = (nickname: string) => {
  return useQuery({
    queryKey: ['userProfileImage', nickname],
    queryFn: () => getUserProfileImageByNickname(nickname),
    enabled: !!nickname,
    staleTime: 1000 * 60 * 5, // 5분 동안 캐시 유지
  });
};
