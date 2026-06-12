package com.example.demo.controller.Market;

import com.example.demo.dto.Market.ProductRequest;
import com.example.demo.dto.Market.ProductRequestDto;
import com.example.demo.dto.Market.ProductResponse;
import com.example.demo.dto.Market.NearbyProductRequest;

import com.example.demo.model.Market.ProductImage;
import com.example.demo.service.Market.ProductService;
import com.example.demo.mapper.Market.ProductImageMapper;
import com.example.demo.util.BaseResponse;
import com.example.demo.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/core/market/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;
    private final JwtTokenProvider jwtTokenProvider;
    private final ProductImageMapper productImageMapper;
    private static final Logger log = LoggerFactory.getLogger(ProductController.class);

    /** 상품 등록 (구매/판매) - 이미지 업로드 포함 **/
    @PostMapping(value = "/registers", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<BaseResponse<ProductResponse>> createProduct(
            @RequestHeader("Authorization") String token,
            @RequestPart("request") ProductRequest request,  // 상품 정보 JSON
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {  // 이미지 파일

        String email = jwtTokenProvider.getUsername(token);
        return productService.createProduct(email, request, images);
    }

    /** 상품 요청 등록 (구매 요청/판매 요청) + 알림 전송  **/
    @PostMapping("/requests")
    public ResponseEntity<BaseResponse<Map<String, Object>>> createRequest(
            @RequestHeader("Authorization") String token, @RequestBody ProductRequestDto request) {

        String email = jwtTokenProvider.getUsername(token);
        return productService.createProductRequestWithChatAndNotification(email, request.getProductId());
    }

    /** 상품 요청 + 채팅방 생성 + 알림 통합 엔드포인트 **/
    @PostMapping("/requests/with-chat")
    public ResponseEntity<BaseResponse<Map<String, Object>>> createRequestWithChat(
            @RequestHeader("Authorization") String token, @RequestBody ProductRequestDto request) {

        String email = jwtTokenProvider.getUsername(token);
        return productService.createProductRequestWithChatAndNotification(email, request.getProductId());
    }

    /** 상품 요청 승인 (등록자만 승인 가능) **/
    @PostMapping("/requests/approve")
    public ResponseEntity<BaseResponse<String>> approveProductRequest(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, Long> requestData) { // JSON 데이터를 받음

        Long productId = requestData.get("productId");
        Long requestId = requestData.get("requestId");

        if (productId == null || requestId == null) {
            return ResponseEntity.badRequest().body(new BaseResponse<>("productId와 requestId는 필수입니다."));
        }

        String email = jwtTokenProvider.getUsername(token);
        return productService.approveProductRequest(email, productId, requestId);
    }

    /** 승인된 요청 목록 조회  **/
    @PostMapping("/requests/approved")
    public ResponseEntity<BaseResponse<Map<String, Object>>> getApprovedRequests(
            @RequestBody Map<String, Long> request) {

        Long productId = request.get("productId"); // JSON에서 productId 추출

        if (productId == null) {
            return ResponseEntity.badRequest()
                    .body(new BaseResponse<>(null, "요청 본문에 productId가 필요합니다."));
        }

        return productService.getApprovedRequests(productId);
    }

    /** 정렬이 안된 모든 상품 조회 - 모집 중인 상품만 조회 **/
    @GetMapping("/all")
    public ResponseEntity<BaseResponse<List<ProductResponse>>> getAllProducts(
            @RequestHeader(value = "Authorization", required = false) String token) {  // 토큰 선택적 처리

        String email = (token != null) ? jwtTokenProvider.getUsername(token) : null;  // 토큰이 있으면 이메일 추출, 없으면 null
        return productService.getAllProducts(email);
    }

    /** 카테고리별 필터 + 가격순/최신순 정렬 - 모집 중인 상품만 조회 **/
    @PostMapping("/all/filter") // GET -> POST 변경 (JSON 데이터를 받기 위함)
    public ResponseEntity<BaseResponse<List<ProductResponse>>> getProducts(
            @RequestBody Map<String, Object> requestData) { // JSON 요청을 받음

        Long categoryId = requestData.containsKey("categoryId") ? ((Number) requestData.get("categoryId")).longValue() : null;
        String sort = (String) requestData.get("sort");

        return ResponseEntity.ok(new BaseResponse<>(productService.getProducts(categoryId, sort)));
    }

    /** 사용자의 위치 기반으로 특정 반경 내(유동적 거리) 있는 상품을 조회 **/
    @PostMapping("/nearby")
    public ResponseEntity<BaseResponse<List<ProductResponse>>> getNearbyProducts(
            @RequestHeader("Authorization") String token,
            @RequestParam int distance) {

        String email = jwtTokenProvider.getUsername(token);
        return productService.getNearbyProducts(
                distance,
                email
        );
    }

    /** 개별 상품 조회 (이미지 포함) - 모집이 끝난 상품은 조회되지 않음 **/
    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<ProductResponse>> getProductById(
            @RequestHeader(value = "Authorization", required = false) String token,  // 토큰 선택적 처리
            @PathVariable Long id) {
        return ResponseEntity.ok(BaseResponse.success(
                productService.getProductById(id, token != null ? jwtTokenProvider.getUsername(token) : null)
        ));
    }

    /** 특정 사용자가 등록한 상품 목록 조회 (구매, 판매, 구매 요청, 판매 요청) **/
    @PostMapping("/users")
    public ResponseEntity<BaseResponse<List<ProductResponse>>> getProductsByUserAndType(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, List<String>> requestBody) {

        String email = jwtTokenProvider.getUsername(token);
        List<String> types = requestBody.get("types");

        return productService.getProductsByUserAndType(email, types);
    }

    /** 내가 등록한 상품 목록 조회 (구매만) **/
    @GetMapping("/users/registers/buy")
    public ResponseEntity<BaseResponse<List<ProductResponse>>> getMyRegisteredBuyProducts(
            @RequestHeader("Authorization") String token) {

        String email = jwtTokenProvider.getUsername(token);
        return productService.getMyRegisteredBuyProducts(email);  // 추가적인 감싸기 제거
    }

    /** 내가 등록한 상품 목록 조회 (판매만) **/
    @GetMapping("/users/registers/sell")
    public ResponseEntity<BaseResponse<List<ProductResponse>>> getMyRegisteredSellProducts(
            @RequestHeader("Authorization") String token) {

        String email = jwtTokenProvider.getUsername(token);
        return productService.getMyRegisteredSellProducts(email);  // 추가적인 감싸기 제거
    }

    /** 내가 요청한 상품 목록 조회 (구매 요청만) **/
    @GetMapping("/users/requests/buy")
    public ResponseEntity<BaseResponse<List<ProductResponse>>> getMyRequestedBuyProducts(
            @RequestHeader("Authorization") String token) {

        String email = jwtTokenProvider.getUsername(token);
        return productService.getMyRequestedBuyProducts(email);  // 추가적인 감싸기 제거
    }

    /** 내가 요청한 상품 목록 조회 (판매 요청만) **/
    @GetMapping("/users/requests/sell")
    public ResponseEntity<BaseResponse<List<ProductResponse>>> getMyRequestedSellProducts(
            @RequestHeader("Authorization") String token) {

        String email = jwtTokenProvider.getUsername(token);
        return productService.getMyRequestedSellProducts(email);  // 추가적인 감싸기 제거
    }

    /** 상품 이미지 직접 반환 (엔드포인트 제공) **/
    @GetMapping("/images/{imageId}")
    public ResponseEntity<Resource> getProductImage(@PathVariable Long imageId) throws IOException {
        ProductImage productImage = productImageMapper.findById(imageId);
        if (productImage == null) {
            return ResponseEntity.notFound().build();
        }

        // 이미지 파일 경로 가져오기
        File file = new File(System.getProperty("user.dir") + "/src/main/resources/static" + productImage.getImagePath());
        if (!file.exists()) {  // 파일 존재 여부 확인
            return ResponseEntity.notFound().build();
        }

        Path path = file.toPath();
        Resource resource = new UrlResource(path.toUri());

        if (!resource.exists() || !resource.isReadable()) {
            return ResponseEntity.notFound().build();
        }

        // **MIME 타입 자동 감지**
        String contentType = Files.probeContentType(path);
        if (contentType == null) {
            contentType = "application/octet-stream"; // 기본 값 설정
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    /**
     * 특정 상품에 대한 사용자의 승인 상태 조회
     */
    @GetMapping("/requests/approval-status")
    public ResponseEntity<BaseResponse<Map<String, String>>> getApprovalStatus(
            @RequestHeader("Authorization") String token,
            @RequestParam Long productId,
            @RequestParam String requestEmail) {
        try {
            log.info("승인 상태 조회 요청: productId={}", productId);
            String email = jwtTokenProvider.getUsername(token);
            
            if (email == null) {
                log.warn("인증되지 않은 사용자의 승인 상태 조회 시도");
                return ResponseEntity.status(401)
                    .body(new BaseResponse<>(null, "인증되지 않은 요청입니다."));
            }

            String approvalStatus = productService.getApprovalStatus(email, productId, requestEmail);
            log.info("승인 상태 조회 결과: productId={}, status={}", productId, approvalStatus);

            Map<String, String> response = new HashMap<>();
            response.put("status", approvalStatus != null ? approvalStatus : "미신청");
            
            return ResponseEntity.ok(new BaseResponse<>(response));
            
        } catch (IllegalArgumentException e) {
            log.warn("승인 상태 조회 중 검증 오류: productId={}, error={}", productId, e.getMessage());
            return ResponseEntity.badRequest()
                .body(new BaseResponse<>(null, e.getMessage()));
        } catch (Exception e) {
            log.error("승인 상태 조회 중 오류 발생: productId={}, error={}", productId, e.getMessage(), e);
            return ResponseEntity.status(500)
                .body(new BaseResponse<>(null, "승인 상태 조회 중 오류가 발생했습니다."));
        }
    }
}




