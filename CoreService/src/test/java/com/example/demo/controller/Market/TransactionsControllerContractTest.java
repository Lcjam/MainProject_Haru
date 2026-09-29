package com.example.demo.controller.Market;

import com.example.demo.dto.Market.PaymentsResponse;
import com.example.demo.dto.Market.TransactionsResponse;
import com.example.demo.exception.GlobalExceptionHandler;
import com.example.demo.service.Market.PaymentsService;
import com.example.demo.service.Market.TransactionsService;
import com.example.demo.util.BaseResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TransactionsControllerContractTest {
    private static final String ACTOR = "buyer@haru.test";

    private TransactionsService transactionsService;
    private PaymentsService paymentsService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        transactionsService = mock(TransactionsService.class);
        paymentsService = mock(PaymentsService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TransactionsController(transactionsService, paymentsService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PrincipalResolver())
                .build();
    }

    @Test
    void everyTransactionEndpointRequiresPrincipal() throws Exception {
        mockMvc.perform(post("/api/core/market/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":100,\"requestId\":200}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("401"));
        mockMvc.perform(get("/api/core/market/transactions/1"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("401"));
        mockMvc.perform(get("/api/core/market/transactions/user"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("401"));
        mockMvc.perform(post("/api/core/market/transactions/1/cancel"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("401"));
        mockMvc.perform(post("/api/core/market/transactions/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"transactionId\":1,\"paymentMethod\":\"카드\"}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("401"));
        mockMvc.perform(get("/api/core/market/transactions/payments/1"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("401"));
    }

    @Test
    void createUsesTokenPrincipalAndKeepsBaseResponseShape() throws Exception {
        given(transactionsService.createTransaction(any(), eq(ACTOR)))
                .willReturn(TransactionsResponse.builder().id(7L).build());

        mockMvc.perform(post("/api/core/market/transactions")
                        .with(actor())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":100,\"requestId\":200,\"buyerEmail\":\"fake\",\"price\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("거래가 생성되었습니다."))
                .andExpect(jsonPath("$.data.id").value(7));

        verify(transactionsService).createTransaction(any(), eq(ACTOR));
    }

    @Test
    void nullJsonBodyIsRejectedAsBadRequest() throws Exception {
        given(transactionsService.createTransaction(isNull(), eq(ACTOR)))
                .willThrow(new IllegalArgumentException("상품과 승인 요청은 필수입니다."));
        given(paymentsService.createPayment(isNull(), eq(ACTOR)))
                .willThrow(new IllegalArgumentException("거래와 유효한 결제 수단은 필수입니다."));

        mockMvc.perform(post("/api/core/market/transactions").with(actor())
                        .contentType(MediaType.APPLICATION_JSON).content("null"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("400"));
        mockMvc.perform(post("/api/core/market/transactions/payments").with(actor())
                        .contentType(MediaType.APPLICATION_JSON).content("null"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("400"));
    }

    @Test
    void detailCancelPaymentAndPaymentListPassTokenPrincipal() throws Exception {
        given(transactionsService.getTransactionById(7L, ACTOR)).willReturn(ResponseEntity.ok(
                new BaseResponse<>(TransactionsResponse.builder().id(7L).build())));
        given(transactionsService.cancelTransaction(7L, ACTOR)).willReturn(ResponseEntity.ok(
                new BaseResponse<>(TransactionsResponse.builder().id(7L).transactionStatus("취소").build())));
        given(transactionsService.getUserTransactions(ACTOR)).willReturn(ResponseEntity.ok(
                new BaseResponse<>(List.of(TransactionsResponse.builder().id(7L).build()))));
        given(paymentsService.createPayment(any(), eq(ACTOR))).willReturn(ResponseEntity.ok(
                new BaseResponse<>(PaymentsResponse.builder().id(9L).build())));
        given(paymentsService.getPaymentsByTransaction(7L, ACTOR)).willReturn(ResponseEntity.ok(
                new BaseResponse<>(List.of(PaymentsResponse.builder().id(9L).build()))));

        mockMvc.perform(get("/api/core/market/transactions/7").with(actor())).andExpect(status().isOk());
        mockMvc.perform(get("/api/core/market/transactions/user").with(actor())).andExpect(status().isOk());
        mockMvc.perform(post("/api/core/market/transactions/7/cancel").with(actor())).andExpect(status().isOk());
        mockMvc.perform(post("/api/core/market/transactions/payments").with(actor())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"transactionId\":7,\"paymentMethod\":\"카드\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/core/market/transactions/payments/7").with(actor()))
                .andExpect(status().isOk());

        verify(transactionsService).getTransactionById(7L, ACTOR);
        verify(transactionsService).getUserTransactions(ACTOR);
        verify(transactionsService).cancelTransaction(7L, ACTOR);
        verify(paymentsService).createPayment(any(), eq(ACTOR));
        verify(paymentsService).getPaymentsByTransaction(7L, ACTOR);
    }

    private RequestPostProcessor actor() {
        UserDetails user = User.withUsername(ACTOR).password("").authorities("ROLE_USER").build();
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user, "token", user.getAuthorities());
        return request -> {
            request.setUserPrincipal(authentication);
            return request;
        };
    }

    private static class PrincipalResolver implements HandlerMethodArgumentResolver {
        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
        }

        @Override
        public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer container,
                                      NativeWebRequest request, WebDataBinderFactory binderFactory) {
            Authentication authentication = (Authentication) request.getUserPrincipal();
            return authentication == null ? null : authentication.getPrincipal();
        }
    }
}
