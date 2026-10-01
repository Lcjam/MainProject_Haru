package com.example.demo.service.Market;

import com.example.demo.dto.Market.ProductRequest;
import com.example.demo.mapper.ChatRoomMapper;
import com.example.demo.mapper.Market.*;
import com.example.demo.model.Market.Product;
import com.example.demo.model.Market.ProductImage;
import com.example.demo.service.ChatService;
import com.example.demo.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ProductImageSecurityTest {
    private final ProductMapper products = mock(ProductMapper.class);
    private final ProductImageMapper images = mock(ProductImageMapper.class);
    private final ImageUploadService uploads = mock(ImageUploadService.class);
    private final ProductService service = new ProductService(products, mock(ProductRequestMapper.class),
            mock(ChatRoomMapper.class), images, mock(NotificationService.class), uploads,
            mock(ChatService.class), mock(TransactionsMapper.class), mock(UserLocationMapper.class));

    @Test
    void hiddenProductImageIsNotPublic() throws Exception {
        when(images.findById(7L)).thenReturn(ProductImage.builder().id(7L).productId(10L)
                .imagePath("/product-images/10/file.png").build());
        when(products.findById(10L, null)).thenReturn(Product.builder().id(10L).isVisible(false).build());

        assertNull(service.getProductImageResource(7L));
        verifyNoInteractions(uploads);
    }

    @Test
    void imageDatabaseFailureRemovesNewProductFiles() {
        doAnswer(invocation -> { ((Product) invocation.getArgument(0)).setId(10L); return null; })
                .when(products).insertProduct(any());
        when(uploads.uploadProductImages(eq("owner@haru.test"), eq(10L), any()))
                .thenReturn(ResponseEntity.ok(Map.of("imagePaths", List.of("/product-images/10/file.png"))));
        doThrow(new IllegalStateException("db failed")).when(images).insertProductImage(any());
        ProductRequest request = new ProductRequest();
        request.setPrice(100);
        var file = new MockMultipartFile("images", "a.png", "image/png", new byte[]{1});

        assertEquals(500, service.createProduct("owner@haru.test", request, List.of(file))
                .getStatusCode().value());
        verify(uploads).deleteProductImages(List.of("/product-images/10/file.png"));
    }
}
