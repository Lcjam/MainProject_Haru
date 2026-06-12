package com.example.demo.dto.Market;

import lombok.*;
import java.time.LocalDateTime;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentsResponse {
    private Long id;
    private Long transactionId;
    private int amount;
    private String paymentMethod;
    private LocalDateTime createdAt;
}