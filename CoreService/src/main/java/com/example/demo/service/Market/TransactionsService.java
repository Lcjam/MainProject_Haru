package com.example.demo.service.Market;

import com.example.demo.dto.Market.TransactionsRequest;
import com.example.demo.dto.Market.TransactionsResponse;
import com.example.demo.mapper.Market.TransactionsMapper;
import com.example.demo.mapper.Market.PaymentsMapper; // 추가됨
import com.example.demo.util.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionsService {

    private final TransactionsMapper transactionsMapper;
    private final PaymentsMapper paymentsMapper;

    /** 거래 생성 **/
    public TransactionsResponse createTransaction(TransactionsRequest request) {
        transactionsMapper.insertTransaction(request);
        return transactionsMapper.findTransactionById(request.getId());
    }

    /** 특정 거래 상세 조회 **/
    public ResponseEntity<BaseResponse<TransactionsResponse>> getTransactionById(Long id) {
        TransactionsResponse transaction = transactionsMapper.findTransactionById(id);
        return ResponseEntity.ok(new BaseResponse<>(transaction));
    }

    /** 사용자별 거래 내역 조회 **/
    public ResponseEntity<BaseResponse<List<TransactionsResponse>>> getUserTransactions(String email) {
        List<TransactionsResponse> transactions = transactionsMapper.findTransactionsByUser(email);
        return ResponseEntity.ok(new BaseResponse<>(transactions));
    }



    /** 결제 완료 시 거래 상태 자동 변경 **/
    @Transactional
    public void updateTransactionStatusOnPayment(Long transactionId) {
        // 거래 금액과 결제 금액 비교
        int totalPaid = paymentsMapper.getTotalPaidByTransaction(transactionId);
        int transactionPrice = transactionsMapper.getTransactionPrice(transactionId);

        // 결제 금액이 충족되었으면 거래 상태를 "완료"로 변경
        if (totalPaid >= transactionPrice) {
            transactionsMapper.updateTransactionStatusOnPayment(transactionId);
        }
    }
}
