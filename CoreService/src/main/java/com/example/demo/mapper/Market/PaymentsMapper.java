package com.example.demo.mapper.Market;

import com.example.demo.dto.Market.PaymentsRequest;
import com.example.demo.dto.Market.PaymentsResponse;
import com.example.demo.model.Market.Payment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PaymentsMapper {
    void insertPayment(PaymentsRequest request);
    List<PaymentsResponse> findPaymentsByTransaction(@Param("transactionId") Long transactionId);
    int getTotalPaidByTransaction(@Param("transactionId") Long transactionId);
}