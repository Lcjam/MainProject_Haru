import { useQuery } from '@tanstack/react-query';
import { axiosInstance } from './axiosInstance';
import { apiConfig } from './apiConfig';

const getCategory = async () => {
  const response = await axiosInstance.get(apiConfig.endpoints.core.getCategory);
  return response.data;
};

export const useCategoryApi = () => {
  const useCategory = () =>
    useQuery({
      queryKey: ['getCategory'],
      queryFn: getCategory
    });
  return { useCategory };
};

export default useCategoryApi;
