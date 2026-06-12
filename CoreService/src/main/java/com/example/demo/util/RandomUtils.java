package com.example.demo.util;

import java.security.SecureRandom;
import java.util.Random;

/**
 * 무작위 코드 생성을 위한 유틸리티 클래스
 */
public class RandomUtils {
    
    private static final String ALPHA_NUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final Random random = new SecureRandom();
    
    /**
     * 지정된 길이의 알파벳+숫자로 구성된 무작위 문자열을 생성합니다.
     * @param length 생성할 문자열의 길이
     * @return 무작위 문자열
     */
    public static String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHA_NUMERIC.charAt(random.nextInt(ALPHA_NUMERIC.length())));
        }
        return sb.toString();
    }
    
    /**
     * 전화번호를 마스킹 처리합니다. (예: 010-1234-5678 → 010-****-****)
     * @param phoneNumber 마스킹 처리할 전화번호
     * @return 마스킹 처리된 전화번호
     */
    public static String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return phoneNumber;
        }
        
        // 형식에 따른 마스킹 처리
        if (phoneNumber.contains("-")) {
            String[] parts = phoneNumber.split("-");
            if (parts.length >= 3) {
                return parts[0] + "-" + "*".repeat(parts[1].length()) + "-" + "*".repeat(parts[2].length());
            }
        }
        
        // 기본 마스킹 처리 (앞 3자리 제외 마스킹)
        if (phoneNumber.length() > 3) {
            return phoneNumber.substring(0, 3) + "*".repeat(phoneNumber.length() - 3);
        }
        
        return phoneNumber;
    }
}
