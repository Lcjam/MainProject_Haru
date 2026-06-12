package com.example.demo.model.Market;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class Payment {
    private Long id;
    private Long transactionId;
    private int amount;
    private String paymentMethod;
    private LocalDateTime createdAt;
}