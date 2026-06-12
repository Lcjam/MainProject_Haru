package com.example.demo.dto.Market;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationMessage {
    private String receiverEmail;
    private String message;
    private String type;
    private Integer chatroomId = 0;
    private Long productId = 0L;

    @Override
    public String toString() {
        return "NotificationMessage{" +
                "receiverEmail='" + receiverEmail + '\'' +
                ", message='" + message + '\'' +
                ", type='" + type + '\'' +
                ", chatroomId=" + chatroomId +
                ", productId=" + productId +
                '}';
    }
}
