package com.example.demo.mapper.Market;

import com.example.demo.dto.Market.PaymentsRequest;
import com.example.demo.dto.Market.PaymentsResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PaymentsMapper {
    void insertPayment(PaymentsRequest request);
    PaymentsResponse findPaymentById(@Param("id") Long id);
    List<PaymentsResponse> findPaymentsByTransaction(@Param("transactionId") Long transactionId);
    long getTotalPaidByTransaction(@Param("transactionId") Long transactionId);
}
