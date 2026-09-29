package com.example.demo.dto.Market;

import lombok.*;

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
