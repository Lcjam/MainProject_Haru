package com.example.demo.controller.Market;

import com.example.demo.dto.Market.TransactionsRequest;
import com.example.demo.dto.Market.TransactionsResponse;
import com.example.demo.dto.Market.PaymentsRequest;
import com.example.demo.dto.Market.PaymentsResponse;
import com.example.demo.service.Market.TransactionsService;
import com.example.demo.service.Market.PaymentsService;
import com.example.demo.util.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/core/market")
@RequiredArgsConstructor
public class TransactionsController {

    private final TransactionsService transactionsService;
    private final PaymentsService paymentsService;

    /** 거래 생성 **/
    @PostMapping("/transactions")
    public ResponseEntity<BaseResponse<TransactionsResponse>> createTransaction(
            @RequestBody TransactionsRequest request) {
        TransactionsResponse response = transactionsService.createTransaction(request);
        return ResponseEntity.ok(new BaseResponse<>(response, "거래가 생성되었습니다."));
    }

    /** 특정 거래 상세 조회 **/
    @GetMapping("/transactions/{id}")
    public ResponseEntity<BaseResponse<TransactionsResponse>> getTransactionById(@PathVariable Long id) {
        return transactionsService.getTransactionById(id);
    }

    /** 사용자별 거래 내역 조회 **/
    @GetMapping("/transactions/user")
    public ResponseEntity<BaseResponse<List<TransactionsResponse>>> getUserTransactions(
            @AuthenticationPrincipal UserDetails userDetails) {  // WT에서 사용자 정보 가져옴

        if (userDetails == null) {
            return ResponseEntity.status(401).body(new BaseResponse<>(null, "인증되지 않은 사용자입니다."));
        }

        String email = userDetails.getUsername(); // JWT에서 이메일 추출
        return transactionsService.getUserTransactions(email);
    }

    /** 결제 요청 (자동으로 상태 업데이트) **/
    @PostMapping("/transactions/payments")
    public ResponseEntity<BaseResponse<PaymentsResponse>> createPayment(@RequestBody PaymentsRequest request) {
        // 결제 요청 시 자동으로 결제 상태 업데이트 실행됨
        ResponseEntity<BaseResponse<PaymentsResponse>> response = paymentsService.createPayment(request);
        return response;
    }

    /** 특정 거래의 결제 내역 조회 **/
    @GetMapping("/transactions/payments/{transactionId}")
    public ResponseEntity<BaseResponse<List<PaymentsResponse>>> getPaymentsByTransaction(@PathVariable Long transactionId) {
        return paymentsService.getPaymentsByTransaction(transactionId);
    }
}