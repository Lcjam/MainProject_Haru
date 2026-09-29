package com.example.demo.service.Market;

import com.example.demo.mapper.ChatRoomMapper;
import com.example.demo.mapper.Market.ProductImageMapper;
import com.example.demo.mapper.Market.ProductMapper;
import com.example.demo.mapper.Market.ProductRequestMapper;
import com.example.demo.mapper.Market.TransactionsMapper;
import com.example.demo.mapper.Market.UserLocationMapper;
import com.example.demo.model.Market.UserLocation;
import com.example.demo.service.ChatService;
import com.example.demo.service.NotificationService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class ProductServiceNearbyLocationTest {
    private final ProductMapper products = mock(ProductMapper.class);
    private final UserLocationMapper locations = mock(UserLocationMapper.class);
    private final ProductService service = new ProductService(products, mock(ProductRequestMapper.class),
            mock(ChatRoomMapper.class), mock(ProductImageMapper.class), mock(NotificationService.class),
            mock(ImageUploadService.class), mock(ChatService.class), mock(TransactionsMapper.class), locations);

    @Test
    void missingOrExpiredLocationReturnsBadRequest() {
        var response = service.getNearbyProducts(1000, "user@haru.com");

        assertEquals(400, response.getStatusCode().value());
        assertEquals("위치 정보가 없습니다.", response.getBody().getMessage());
        verifyNoInteractions(products);
    }

    @Test
    void nearbyUsesOneLatestLocationRead() {
        given(locations.getUserLatestLocation("user@haru.com")).willReturn(
                UserLocation.builder().latitude(37.5).longitude(127.0).build());
        given(products.findNearbyProducts(37.5, 127.0, 1000)).willReturn(List.of());

        var response = service.getNearbyProducts(1000, "user@haru.com");

        assertEquals(200, response.getStatusCode().value());
        verify(products).findNearbyProducts(37.5, 127.0, 1000);
        verify(locations).getUserLatestLocation("user@haru.com");
        verifyNoMoreInteractions(locations);
    }
}
