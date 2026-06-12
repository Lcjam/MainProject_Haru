import { uploadInstance } from './axiosInstance';
import { apiConfig } from './apiConfig';

interface FileResponse {
  status: string;
  data: {
    message: string;
    response: string;
  };
  code: string;
}

export const CloudOCR = async (formData: FormData): Promise<FileResponse> => {
  try {
    const response = await uploadInstance.post(apiConfig.endpoints.assist.cloudOCR, formData, {
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
    console.error('OCR API Error:', error);
    return {
      status: 'error',
      data: {
        message: error instanceof Error ? error.message : '이미지 처리 중 오류가 발생했습니다.',
        response: ''
      },
      code: '500'
    };
  }
};

// assist sever
//=============================================================================================
export const assistProfile = async (formData: FormData): Promise<FileResponse> => {
  try {
    const response = await uploadInstance.post(apiConfig.endpoints.assist.uploadProfile, formData, {
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
        response: ''
      },
      code: '500'
    };
  }
};

export const fileUpload = {
  CloudOCR,
  assistProfile
};
