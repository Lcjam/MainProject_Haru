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
    TransactionsResponse findTransactionByProductAndParticipants(
            @Param("productId") Long productId,
            @Param("buyerEmail") String buyerEmail,
            @Param("sellerEmail") String sellerEmail);
    TransactionsResponse findTransactionByIdForUser(
            @Param("id") Long id, @Param("email") String email);
    TransactionsResponse findTransactionByIdForUserForUpdate(
            @Param("id") Long id, @Param("email") String email);
    List<TransactionsResponse> findTransactionsByUser(@Param("email") String email);
    int completeTransaction(@Param("id") Long id);
    int cancelTransaction(@Param("id") Long id);
}
