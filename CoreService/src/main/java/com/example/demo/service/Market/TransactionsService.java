package com.example.demo.service.Market;

import com.example.demo.dto.Market.TransactionsRequest;
import com.example.demo.dto.Market.TransactionsResponse;
import com.example.demo.exception.ForbiddenException;
import com.example.demo.exception.NotFoundException;
import com.example.demo.mapper.Market.PaymentsMapper;
import com.example.demo.mapper.Market.ProductMapper;
import com.example.demo.mapper.Market.ProductRequestMapper;
import com.example.demo.mapper.Market.TransactionsMapper;
import com.example.demo.model.Market.Product;
import com.example.demo.model.Market.ProductRequest;
import com.example.demo.util.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class TransactionsService {

    private final TransactionsMapper transactionsMapper;
    private final ProductMapper productMapper;
    private final ProductRequestMapper productRequestMapper;
    private final PaymentsMapper paymentsMapper;

    @Transactional
    public TransactionsResponse createTransaction(TransactionsRequest request, String actorEmail) {
        if (request == null || request.getProductId() == null || request.getRequestId() == null) {
            throw new IllegalArgumentException("상품과 승인 요청은 필수입니다.");
        }

        Product product = productMapper.findByIdForUpdate(request.getProductId());
        if (product == null) {
            throw new NotFoundException("거래 대상을 찾을 수 없습니다.");
        }
        if (!Objects.equals(product.getEmail(), actorEmail)) {
            throw new ForbiddenException("해당 상품의 등록자만 거래를 생성할 수 있습니다.");
        }

        ProductRequest approvedRequest = productRequestMapper.findByIdForUpdate(request.getRequestId());
        if (approvedRequest == null || !Objects.equals(product.getId(), approvedRequest.getProductId())
                || !"승인".equals(approvedRequest.getApprovalStatus())) {
            throw new NotFoundException("거래 대상을 찾을 수 없습니다.");
        }

        boolean ownerSells = "판매".equals(product.getRegistrationType());
        String buyerEmail = ownerSells ? approvedRequest.getRequesterEmail() : product.getEmail();
        String sellerEmail = ownerSells ? product.getEmail() : approvedRequest.getRequesterEmail();
        TransactionsResponse existing = transactionsMapper.findTransactionByProductAndParticipants(
                product.getId(), buyerEmail, sellerEmail);
        if (existing != null) {
            return existing;
        }

        TransactionsRequest serverRequest = TransactionsRequest.builder()
                .productId(product.getId())
                .buyerEmail(buyerEmail)
                .sellerEmail(sellerEmail)
                .price(product.getPrice())
                .description("상품 요청 승인으로 생성된 거래")
                .build();
        transactionsMapper.insertTransaction(serverRequest);
        return transactionsMapper.findTransactionById(serverRequest.getId());
    }

    public ResponseEntity<BaseResponse<TransactionsResponse>> getTransactionById(Long id, String actorEmail) {
        TransactionsResponse transaction = transactionsMapper.findTransactionByIdForUser(id, actorEmail);
        if (transaction == null) {
            throw new NotFoundException("거래를 찾을 수 없습니다.");
        }
        return ResponseEntity.ok(new BaseResponse<>(transaction));
    }

    public ResponseEntity<BaseResponse<List<TransactionsResponse>>> getUserTransactions(String actorEmail) {
        return ResponseEntity.ok(new BaseResponse<>(transactionsMapper.findTransactionsByUser(actorEmail)));
    }

    @Transactional
    public ResponseEntity<BaseResponse<TransactionsResponse>> cancelTransaction(Long id, String actorEmail) {
        TransactionsResponse transaction = transactionsMapper.findTransactionByIdForUserForUpdate(id, actorEmail);
        if (transaction == null) {
            throw new NotFoundException("거래를 찾을 수 없습니다.");
        }
        if (!"진행중".equals(transaction.getTransactionStatus())
                || !"미완료".equals(transaction.getPaymentStatus())
                || paymentsMapper.getTotalPaidByTransaction(id) != 0) {
            throw new IllegalArgumentException("결제 전 진행 중인 거래만 취소할 수 있습니다.");
        }
        if (transactionsMapper.cancelTransaction(id) != 1) {
            throw new IllegalStateException("거래 취소 상태를 저장하지 못했습니다.");
        }
        return ResponseEntity.ok(new BaseResponse<>(
                transactionsMapper.findTransactionById(id), "거래가 취소되었습니다."));
    }
}
