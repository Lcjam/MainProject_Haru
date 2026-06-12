import { mockLatestProducts, mockRecommendedProducts, IProduct } from './mockData';

// 실제 API 응답 구조를 모방
const createResponse = <T>(data: T) => ({
  status: 'success',
  data
});

export const mockAPI = {
  market: {
    // 최신 상품 목록 조회
    getLatestProducts: async () => {
      await new Promise((resolve) => setTimeout(resolve, 500));
      return createResponse({
        products: mockLatestProducts
      });
    },

    // 추천 상품 목록 조회
    getRecommendedProducts: async () => {
      await new Promise((resolve) => setTimeout(resolve, 500));
      return createResponse({
        products: mockRecommendedProducts
      });
    },

    // 카테고리별 상품 조회
    getProductsByCategory: async (category: string) => {
      await new Promise((resolve) => setTimeout(resolve, 500));
      const allProducts = [...mockLatestProducts, ...mockRecommendedProducts];
      
      // 전체 카테고리인 경우 모든 상품 반환
      if (category === '전체') {
        return createResponse({
          products: allProducts
        });
      }
      
      // 카테고리명으로 필터링
      const filteredProducts = allProducts.filter(product => product.category === category);
      return createResponse({
        products: filteredProducts
      });
    },

    // 상품 상세 조회
    getProductById: async (id: number) => {
      await new Promise((resolve) => setTimeout(resolve, 500));
      const product = [...mockLatestProducts, ...mockRecommendedProducts]
        .find(product => product.id === id);
      
      if (!product) {
        throw new Error('상품을 찾을 수 없습니다.');
      }

      return createResponse({
        product
      });
    },

    // 상품 등록
    createProduct: async (productData: Omit<IProduct, 'id'>) => {
      await new Promise((resolve) => setTimeout(resolve, 1000));
      const newProduct = {
        id: Math.floor(Math.random() * 10000),
        ...productData
      };
      return createResponse({
        product: newProduct
      });
    }
  }
};

// 테스트에서 사용할 수 있는 헬퍼 함수
export const setupMockAPI = () => {
  // axios mock adapter 설정 등
  console.log('Mock API가 설정되었습니다.');
};
