package com.example.demo.dto.Market;

import lombok.*;
import java.time.LocalDateTime;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class TransactionsResponse {
    private Long id;
    private Long productId;
    private String request_email;
    private String sellerEmail;
    private String transactionStatus;
    private String paymentStatus;
    private int price;
    private String description;
    private LocalDateTime createdAt;
}
