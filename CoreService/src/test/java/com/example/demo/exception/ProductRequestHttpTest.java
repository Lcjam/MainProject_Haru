package com.example.demo.exception;

import com.example.demo.config.OAuth2Config;
import com.example.demo.config.SecurityConfig;
import com.example.demo.controller.Market.ProductController;
import com.example.demo.dto.chat.ChatRoomResponse;
import com.example.demo.mapper.ChatRoomMapper;
import com.example.demo.mapper.Market.*;
import com.example.demo.model.Market.Product;
import com.example.demo.security.JwtAuthenticationEntryPoint;
import com.example.demo.security.JwtTokenBlacklistService;
import com.example.demo.security.JwtTokenProvider;
import com.example.demo.service.ChatService;
import com.example.demo.service.Market.ImageUploadService;
import com.example.demo.service.Market.ProductService;
import com.example.demo.service.NotificationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = ProductRequestHttpTest.TestApplication.class)
class ProductRequestHttpTest {
    private static final String SECRET = "core-c01-product-request-test-secret";
    private static final String REQUESTER = "requester@example.com";
    private final HttpClient client = HttpClient.newHttpClient();
    @LocalServerPort int port;
    @Autowired ObjectMapper json;
    @MockBean ProductMapper products;
    @MockBean ProductRequestMapper requests;
    @MockBean ChatRoomMapper rooms;
    @MockBean ProductImageMapper images;
    @MockBean NotificationService notifications;
    @MockBean ImageUploadService uploads;
    @MockBean ChatService chats;
    @MockBean TransactionsMapper transactions;
    @MockBean UserLocationMapper locations;
    @MockBean JwtTokenBlacklistService blacklist;
    @MockBean OAuth2Config oauth;

    @BeforeEach
    void productExists() {
        when(products.findEmailByProductId(7L)).thenReturn("owner@example.com");
    }

    @ParameterizedTest
    @ValueSource(strings = {"/requests", "/requests/with-chat"})
    void firstRequestKeepsSuccessShapeAndSecondRequestHasNoSideEffects(String path) throws Exception {
        when(requests.findRequestId(7L, REQUESTER)).thenReturn(null, 99L);
        when(products.findById(7L, REQUESTER)).thenReturn(Product.builder()
                .id(7L).email("owner@example.com").title("테스트 상품").registrationType("판매")
                .currentParticipants(1).maxParticipants(3).isVisible(true).build());
        when(chats.createOrGetChatRoom(eq(REQUESTER), any())).thenReturn(ChatRoomResponse.builder()
                .success(true).chatroomId(42).productId(7L).build());

        var first = send(path);
        assertEquals(200, first.statusCode(), first.body());
        JsonNode body = json.readTree(first.body());
        Set<String> fields = new HashSet<>();
        body.fieldNames().forEachRemaining(fields::add);
        assertEquals(Set.of("status", "message", "data"), fields);
        assertEquals("success", body.path("status").asText());
        assertEquals("상품 요청 및 채팅방 생성이 완료되었습니다.", body.path("message").asText());
        assertEquals(7L, body.at("/data/requestInfo/productId").asLong());
        assertEquals(REQUESTER, body.at("/data/requestInfo/requesterEmail").asText());
        assertEquals("대기", body.at("/data/requestInfo/status").asText());
        assertEquals("미승인", body.at("/data/requestInfo/approvalStatus").asText());
        assertEquals("구매 요청", body.at("/data/requestInfo/requestType").asText());
        assertEquals(42, body.at("/data/chatInfo/chatroomId").asInt());
        assertEquals(7L, body.at("/data/productInfo/id").asLong());
        verify(products).insertProductRequest(7L, REQUESTER);
        verify(products).increaseCurrentParticipants(7L);
        verify(products).updateProductVisibility(7L);
        verify(notifications).sendNotification("owner@example.com",
                "\"테스트 상품\" 상품에 대한 새로운 신청이 도착했습니다!", "PRODUCT_REQUEST", 0, 7L);
        verify(chats).createOrGetChatRoom(eq(REQUESTER), argThat(request -> request.getProductId().equals(7L)));

        clearInvocations(products, requests, notifications, chats, rooms, images);
        assertError(send(path), 409, "CONFLICT", "이미 신청한 상품입니다.");
        verify(requests).findRequestId(7L, REQUESTER);
        verifyNoMoreInteractions(requests);
        assertNoSideEffects();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/requests", "/requests/with-chat"})
    void existingRequestReturnsConflictWithoutSideEffects(String path) throws Exception {
        when(requests.findRequestId(7L, REQUESTER)).thenReturn(99L);

        assertError(send(path), 409, "CONFLICT", "이미 신청한 상품입니다.");
        verify(requests).findRequestId(7L, REQUESTER);
        verifyNoMoreInteractions(requests);
        assertNoSideEffects();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/requests", "/requests/with-chat"})
    void missingProductKeepsBadRequestWithoutSideEffects(String path) throws Exception {
        when(products.findEmailByProductId(7L)).thenReturn(null);

        assertError(send(path), 400, "BAD_REQUEST", "해당 상품을 찾을 수 없습니다.");
        verifyNoInteractions(requests);
        assertNoSideEffects();
    }

    private void assertNoSideEffects() {
        verify(products).findEmailByProductId(7L);
        verifyNoMoreInteractions(products);
        verifyNoInteractions(notifications, chats, rooms, images, uploads, transactions, locations);
    }

    private void assertError(HttpResponse<String> response, int status, String code, String message) throws Exception {
        assertEquals(status, response.statusCode(), response.body());
        JsonNode body = json.readTree(response.body());
        assertEquals("error", body.path("status").asText());
        assertEquals(code, body.path("code").asText());
        assertEquals(message, body.path("message").asText());
    }

    private HttpResponse<String> send(String path) throws Exception {
        String token = new JwtTokenProvider(SECRET, 60000).createToken(1, REQUESTER, List.of("ROLE_USER"));
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/core/market/products" + path))
                .header("Authorization", "Bearer " + token).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"productId\":7}")).build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @SpringBootConfiguration(proxyBeanMethods = false)
    @EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class, RedisAutoConfiguration.class,
            RedisRepositoriesAutoConfiguration.class, SecurityAutoConfiguration.class, OAuth2ClientAutoConfiguration.class})
    @Import({SecurityConfig.class, ProductController.class, ProductService.class,
            GlobalExceptionHandler.class, ApiErrorResponseAdvice.class, ApiErrorController.class})
    static class TestApplication {
        @Bean JwtTokenProvider tokens() { return new JwtTokenProvider(SECRET, 60000); }
        @Bean JwtAuthenticationEntryPoint entryPoint() { return new JwtAuthenticationEntryPoint(); }
    }
}
