package com.example.demo.service.Market;

import com.example.demo.dto.Market.PaymentsRequest;
import com.example.demo.dto.Market.PaymentsResponse;
import com.example.demo.dto.Market.TransactionsResponse;
import com.example.demo.exception.ForbiddenException;
import com.example.demo.exception.NotFoundException;
import com.example.demo.mapper.Market.PaymentsMapper;
import com.example.demo.mapper.Market.TransactionsMapper;
import com.example.demo.util.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PaymentsService {

    private static final Set<String> PAYMENT_METHODS = Set.of("포인트", "카드", "계좌이체");

    private final PaymentsMapper paymentsMapper;
    private final TransactionsMapper transactionsMapper;

    @Transactional
    public ResponseEntity<BaseResponse<PaymentsResponse>> createPayment(
            PaymentsRequest request, String actorEmail) {
        if (request == null || request.getTransactionId() == null || request.getPaymentMethod() == null
                || !PAYMENT_METHODS.contains(request.getPaymentMethod())) {
            throw new IllegalArgumentException("거래와 유효한 결제 수단은 필수입니다.");
        }

        TransactionsResponse transaction = transactionsMapper.findTransactionByIdForUserForUpdate(
                request.getTransactionId(), actorEmail);
        if (transaction == null) {
            throw new NotFoundException("거래를 찾을 수 없습니다.");
        }
        if (!actorEmail.equals(transaction.getBuyerEmail())) {
            throw new ForbiddenException("구매자만 결제할 수 있습니다.");
        }
        if (!"진행중".equals(transaction.getTransactionStatus())
                || !"미완료".equals(transaction.getPaymentStatus())) {
            throw new IllegalArgumentException("진행 중인 미결제 거래만 결제할 수 있습니다.");
        }

        long totalPaid = paymentsMapper.getTotalPaidByTransaction(request.getTransactionId());
        long remaining = (long) transaction.getPrice() - totalPaid;
        if (totalPaid < 0 || remaining <= 0) {
            throw new IllegalArgumentException("결제 금액이 거래 금액을 초과했거나 이미 결제되었습니다.");
        }

        request.setAmount((int) remaining);
        paymentsMapper.insertPayment(request);
        if (transactionsMapper.completeTransaction(request.getTransactionId()) != 1) {
            throw new IllegalStateException("거래 결제 상태를 저장하지 못했습니다.");
        }

        PaymentsResponse payment = paymentsMapper.findPaymentById(request.getId());
        if (payment == null) {
            throw new IllegalStateException("생성된 결제 정보를 찾을 수 없습니다.");
        }
        return ResponseEntity.ok(new BaseResponse<>(payment, "결제가 성공적으로 처리되었습니다."));
    }

    public ResponseEntity<BaseResponse<List<PaymentsResponse>>> getPaymentsByTransaction(
            Long transactionId, String actorEmail) {
        if (transactionsMapper.findTransactionByIdForUser(transactionId, actorEmail) == null) {
            throw new NotFoundException("거래를 찾을 수 없습니다.");
        }
        return ResponseEntity.ok(new BaseResponse<>(
                paymentsMapper.findPaymentsByTransaction(transactionId)));
    }
}
