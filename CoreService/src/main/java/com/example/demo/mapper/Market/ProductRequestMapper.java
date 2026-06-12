package com.example.demo.mapper.Market;

import com.example.demo.model.Market.ProductRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface ProductRequestMapper {
    
    /**
     * 상품 ID와 요청자 이메일로 요청 정보 조회
     */
    ProductRequest findByProductIdAndRequesterEmail(
        @Param("productId") Long productId, 
        @Param("requesterEmail") String requesterEmail);
    
    /**
     * 상품 ID와 요청자 이메일로 요청 ID 조회
     */
    Long findRequestId(
        @Param("productId") Long productId, 
        @Param("requesterEmail") String requesterEmail);
    
    /**
     * 상품 ID로 모든 요청 조회
     */
    List<ProductRequest> findByProductId(@Param("productId") Long productId);
    
    /**
     * 상품 ID로 첫 번째 요청 ID 조회
     */
    Long findFirstRequestIdByProductId(@Param("productId") Long productId);
}
