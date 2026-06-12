package com.example.demo.mapper.Market;

import com.example.demo.dto.Market.TransactionsRequest;
import com.example.demo.dto.Market.TransactionsResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TransactionsMapper {
    void insertTransaction(TransactionsRequest request);
    TransactionsResponse findTransactionById(@Param("id") Long id);
    List<TransactionsResponse> findTransactionsByUser(@Param("email") String email);
    int getTransactionPrice(@Param("id") Long id);
    void updateTransactionStatusOnPayment(@Param("transactionId") Long transactionId);
}
