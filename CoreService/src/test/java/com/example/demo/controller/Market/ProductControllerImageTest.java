package com.example.demo.controller.Market;

import com.example.demo.security.JwtTokenProvider;
import com.example.demo.service.Market.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ProductController 이미지 엔드포인트의 HTTP 계약을 고정한다.
 * 단계 4에서 파일 I/O 로직을 ProductService 로 이관하면서, 컨트롤러가
 * 서비스 결과를 200(헤더 포함)/404 로 올바르게 매핑하는지 보장한다.
 * 보안 필터 슬라이스 얽힘을 피하기 위해 standalone MockMvc + Mockito 로 컨트롤러만 검증.
 */
@DisplayName("ProductController 이미지 엔드포인트 contract")
class ProductControllerImageTest {

    private final ProductService productService = mock(ProductService.class);
    private final JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ProductController(productService, jwtTokenProvider))
                .build();
    }

    @Test
    @DisplayName("이미지가 있으면 200 + Content-Type + inline Content-Disposition + 본문으로 응답한다")
    void getProductImage_found_returnsResourceWithHeaders() throws Exception {
        byte[] bytes = {1, 2, 3, 4};
        Resource resource = new ByteArrayResource(bytes);
        given(productService.getProductImageResource(7L))
                .willReturn(new ProductService.ProductImageFile(resource, "image/png", "photo.png"));

        mockMvc.perform(get("/api/core/market/products/images/{imageId}", 7))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"photo.png\""))
                .andExpect(content().bytes(bytes));
    }

    @Test
    @DisplayName("이미지 메타/파일이 없으면(서비스 null) 404 를 반환한다")
    void getProductImage_missing_returns404() throws Exception {
        given(productService.getProductImageResource(999L)).willReturn(null);

        mockMvc.perform(get("/api/core/market/products/images/{imageId}", 999))
                .andExpect(status().isNotFound());
    }
}
