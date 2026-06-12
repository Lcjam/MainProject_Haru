package com.example.demo.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Component
public class PasswordUtils {

    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    public PasswordUtils() {
        // BCrypt 사용을 위한 인코더 생성 (보안 강도 12)
        this.bCryptPasswordEncoder = new BCryptPasswordEncoder(12);
    }

    /**
     * 고유한 솔트를 생성합니다.
     * BCrypt는 내부적으로 솔트를 관리하지만, 기존 DB 구조를 유지하기 위해 사용
     */
    public String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * 비밀번호를 BCrypt로 해싱합니다.
     * 솔트는 BCrypt가 내부적으로 처리하지만, 기존 DB 구조 호환성을 위해 파라미터로 받습니다.
     */
    public Map<String, String> hashPassword(String password, String salt) {
        // BCrypt에서는 솔트를 명시적으로 전달하지 않음 - BCrypt가 자동 생성하고 해시에 포함시킴
        String hashedPassword = bCryptPasswordEncoder.encode(password);

        Map<String, String> result = new HashMap<>();

        result.put("hashedPassword", hashedPassword);
        result.put("salt", salt);
        return result;
    }

    /**
     * 입력된 비밀번호가 저장된 해시와 일치하는지 BCrypt로 확인합니다.
     * 솔트 파라미터는 기존 API 호환성을 위해 유지하지만 사용하지 않습니다.
     */
    public boolean verifyPassword(String inputPassword, String storedHash, String salt) {
        // BCrypt는 솔트를 해시 문자열에 포함하므로 별도 전달 불필요
        return bCryptPasswordEncoder.matches(inputPassword, storedHash);
    }
}