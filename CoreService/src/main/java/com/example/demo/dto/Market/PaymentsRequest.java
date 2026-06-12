package com.example.demo.dto.Market;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentsRequest {
    private Long id;
    private Long transactionId;
    private int amount;
    private String paymentMethod;
}