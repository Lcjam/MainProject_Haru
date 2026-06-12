package com.example.demo.mapper.Market;

import com.example.demo.dto.Market.ProductResponse;
import com.example.demo.model.Market.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Mapper
@Repository
public interface ProductMapper {

    // 상품 추가
    void insertProduct(Product product);


    // 상품 조회 (등록자는 마감된 상품도 조회가능)
    Product findById(@Param("id") Long id, @Param("email") String email);

    // 특정 카테고리 필터링 + 정렬 추가
    List<Product> findFilteredProducts(@Param("categoryId") Long categoryId, @Param("sort") String sort);

    // 사용자(이메일)가 등록한 상품별 목록 조회 추가 (구매, 판매, 구매 요청, 판매 요청)
    List<Product> findProductsByEmailAndType(@Param("email") String email, @Param("types") List<String> types);

    // 특정 상품의 현재 참여 인원 조회 (current_participants 값 가져오기)
    int getCurrentParticipants(@Param("productId") Long productId);

    // 상품 요청 등록 (ProductRequests 테이블에 저장) - 요청자 이메일 추가
    void insertProductRequest(@Param("productId") Long productId, @Param("requesterEmail") String requesterEmail);

    // 모집 인원 증가
    void increaseCurrentParticipants(@Param("productId") Long productId);

    // 모집 완료 시 상품 비활성화
    void updateProductVisibility(@Param("productId") Long productId);

    // 모집 마감 체크
    void updateProductStatusToComplete(Long productId);

    // 상품 요청 승인 여부 업데이트
    void updateRequestApprovalStatus(@Param("requestId") Long requestId, @Param("approvalStatus") String approvalStatus);

    // 상품 상태를 인원 충족 시 자동으로 '완료'로 변경하는 메서드
    void updateRequestStatusToComplete(@Param("productId") Long productId);

    // 상품 ID를 이용해 등록한 사용자의 이메일 찾기 (알림 전송 시 사용)
    String findEmailByProductId(@Param("productId") Long productId);

    // 승인된 요청만 가져오기
    List<Map<String, Object>> findApprovedRequests(@Param("productId") Long productId);

    // 내가 등록한 상품 목록 조회 (구매만)
    List<Product> findMyRegisteredBuyProducts(@Param("email") String email);

    // 내가 등록한 상품 목록 조회 (판매만)
    List<Product> findMyRegisteredSellProducts(@Param("email") String email);

    // 내가 요청한 상품 목록 조회 (구매 요청만)
    List<Product> findMyRequestedBuyProducts(@Param("email") String email);

    // 내가 요청한 상품 목록 조회 (판매 요청만)
    List<Product> findMyRequestedSellProducts(@Param("email") String email);

    // 요청자의 이메일을 요청 ID(requestId)를 통해 조회
    String findRequesterEmailByRequestId(@Param("requestId") Long requestId);

    // 전체 상품 조회
    List<Product> findAll(@Param("email") String email);

    // 특정 반경 내(유동적 거리) 있는 상품 조회
    List<ProductResponse> findNearbyProducts(@Param("latitude") double latitude,
                                             @Param("longitude") double longitude,
                                             @Param("distance") double distance);

    // 특정 상품에 대한 사용자의 승인 상태 조회
    String findApprovalStatus(@Param("email") String email, @Param("productId") Long productId);

}

