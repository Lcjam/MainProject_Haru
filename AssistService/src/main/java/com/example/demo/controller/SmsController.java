package com.example.demo.controller;

import com.example.demo.serviceimpl.SmsService;

import net.nurigo.sdk.message.response.SingleMessageSentResponse;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/assist/sms")

public class SmsController {

  @Autowired
  private SmsService smsService;

  @PostMapping("/send-sms")
  public ResponseEntity<Map<String, Object>> sendSms(@RequestBody Map<String, String> requestBody) {
    String phoneNumber = requestBody.get("phoneNumber");
    SingleMessageSentResponse response = smsService.sendSms(phoneNumber);

    Map<String, Object> responseBody = new HashMap<>();
    responseBody.put("statusCode", response.getStatusCode());
    responseBody.put("message", response.getStatusMessage());

    return ResponseEntity.ok(responseBody); // HTTP 200 상태 코드와 함께 응답 반환
  }

  @PostMapping("/verify-otp")
  public ResponseEntity<Map<String, Object>> verifyOtp(@RequestBody Map<String, String> requestBody) {
    System.out.println("요청받은 데이터: " + requestBody);

    String phoneNumber = requestBody.get("phoneNumber");
    String otp = requestBody.get("otp");
    System.out.println("phoneNumber: " + phoneNumber + ", otp: " + otp);

    boolean isVerified = smsService.verifyCode(phoneNumber, otp);

    Map<String, Object> responseBody = new HashMap<>();
    responseBody.put("success", isVerified);
    responseBody.put("message", isVerified ? "success" : "fail");
    System.out.println("responseBody: " + responseBody);

    return ResponseEntity.ok(responseBody);
  }

}
