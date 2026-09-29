package com.example.demo.dto.Market;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionsRequest {
    private Long id;
    private Long productId;
    private Long requestId;
    private String buyerEmail;
    private String sellerEmail;
    private int price;
    private String description;
}
