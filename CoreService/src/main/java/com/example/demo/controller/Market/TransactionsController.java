package com.example.demo.controller.Market;

import com.example.demo.dto.Market.PaymentsRequest;
import com.example.demo.dto.Market.PaymentsResponse;
import com.example.demo.dto.Market.TransactionsRequest;
import com.example.demo.dto.Market.TransactionsResponse;
import com.example.demo.exception.UnauthorizedException;
import com.example.demo.service.Market.PaymentsService;
import com.example.demo.service.Market.TransactionsService;
import com.example.demo.util.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/core/market")
@RequiredArgsConstructor
public class TransactionsController {

    private final TransactionsService transactionsService;
    private final PaymentsService paymentsService;

    @PostMapping("/transactions")
    public ResponseEntity<BaseResponse<TransactionsResponse>> createTransaction(
            @RequestBody(required = false) TransactionsRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        TransactionsResponse response = transactionsService.createTransaction(request, actor(principal));
        return ResponseEntity.ok(new BaseResponse<>(response, "거래가 생성되었습니다."));
    }

    @GetMapping("/transactions/{id}")
    public ResponseEntity<BaseResponse<TransactionsResponse>> getTransactionById(
            @PathVariable Long id, @AuthenticationPrincipal UserDetails principal) {
        return transactionsService.getTransactionById(id, actor(principal));
    }

    @GetMapping("/transactions/user")
    public ResponseEntity<BaseResponse<List<TransactionsResponse>>> getUserTransactions(
            @AuthenticationPrincipal UserDetails principal) {
        return transactionsService.getUserTransactions(actor(principal));
    }

    @PostMapping("/transactions/{id}/cancel")
    public ResponseEntity<BaseResponse<TransactionsResponse>> cancelTransaction(
            @PathVariable Long id, @AuthenticationPrincipal UserDetails principal) {
        return transactionsService.cancelTransaction(id, actor(principal));
    }

    @PostMapping("/transactions/payments")
    public ResponseEntity<BaseResponse<PaymentsResponse>> createPayment(
            @RequestBody(required = false) PaymentsRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        return paymentsService.createPayment(request, actor(principal));
    }

    @GetMapping("/transactions/payments/{transactionId}")
    public ResponseEntity<BaseResponse<List<PaymentsResponse>>> getPaymentsByTransaction(
            @PathVariable Long transactionId,
            @AuthenticationPrincipal UserDetails principal) {
        return paymentsService.getPaymentsByTransaction(transactionId, actor(principal));
    }

    private String actor(UserDetails principal) {
        if (principal == null) {
            throw new UnauthorizedException("인증되지 않은 사용자입니다.");
        }
        return principal.getUsername();
    }
}
