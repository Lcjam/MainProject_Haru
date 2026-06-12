package com.example.demo.service.Market;

import com.example.demo.dto.Market.PaymentsRequest;
import com.example.demo.dto.Market.PaymentsResponse;
import com.example.demo.mapper.Market.TransactionsMapper;
import com.example.demo.mapper.Market.PaymentsMapper;
import com.example.demo.util.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentsService {

    private final PaymentsMapper paymentsMapper;
    private final TransactionsMapper transactionsMapper;
    private final TransactionsService transactionsService;

    /**
     * 결제 요청 (자동 상태 업데이트 포함)
     **/
    @Transactional
    public ResponseEntity<BaseResponse<PaymentsResponse>> createPayment(PaymentsRequest request) {
        // 결제 요청을 DB에 저장
        paymentsMapper.insertPayment(request);

        // 결제 상태 자동 업데이트
        updatePaymentStatus(request.getTransactionId());


        // 거래 상태도 자동 업데이트
        transactionsService.updateTransactionStatusOnPayment(request.getTransactionId());

        // 결제 내역 조회 후 반환 (결과가 없는 경우 예외 방지)
        List<PaymentsResponse> payments = paymentsMapper.findPaymentsByTransaction(request.getTransactionId());
        if (payments.isEmpty()) {
            return ResponseEntity.badRequest().body(new BaseResponse<>(null, "결제 정보를 찾을 수 없습니다."));
        }

        return ResponseEntity.ok(new BaseResponse<>(payments.get(0), "결제가 성공적으로 처리되었습니다."));
    }

    /**
     * 특정 거래의 결제 내역 조회
     **/
    public ResponseEntity<BaseResponse<List<PaymentsResponse>>> getPaymentsByTransaction(Long transactionId) {
        List<PaymentsResponse> payments = paymentsMapper.findPaymentsByTransaction(transactionId);
        return ResponseEntity.ok(new BaseResponse<>(payments));
    }

    /**
     * 결제 완료 여부 확인 후 거래 상태 자동 변경
     **/
    // 결제 상태 자동 업데이트 메서드
    private void updatePaymentStatus(Long transactionId) {
        int totalPaid = paymentsMapper.getTotalPaidByTransaction(transactionId);
        int transactionPrice = transactionsMapper.getTransactionPrice(transactionId);

        if (totalPaid >= transactionPrice) {
            transactionsMapper.updateTransactionStatusOnPayment(transactionId); // 여기서 "완료"로 업데이트됨!
        }
    }
}
