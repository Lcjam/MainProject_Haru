package com.example.demo.dto.auth;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 인증 요청 DTO 의 Bean Validation 제약 검증 (Spring 컨텍스트/DB 불필요한 순수 단위 테스트).
 * Phase 2A 에서 추가한 @NotBlank/@Email/@Size 제약이 실제로 동작하는지 보장한다.
 */
class AuthRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    @DisplayName("LoginRequest: 이메일/비밀번호가 비면 위반이 발생한다")
    void loginRequest_blank_hasViolations() {
        LoginRequest req = new LoginRequest();
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(req);
        assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("LoginRequest: 잘못된 이메일 형식은 email 위반을 만든다")
    void loginRequest_invalidEmail_hasEmailViolation() {
        LoginRequest req = new LoginRequest();
        req.setEmail("not-an-email");
        req.setPassword("secret");
        assertThat(validator.validate(req))
                .anyMatch(v -> v.getPropertyPath().toString().equals("email"));
    }

    @Test
    @DisplayName("LoginRequest: 유효한 값은 위반이 없다")
    void loginRequest_valid_noViolations() {
        LoginRequest req = new LoginRequest();
        req.setEmail("user@haru.com");
        req.setPassword("secret");
        assertThat(validator.validate(req)).isEmpty();
    }

    @Test
    @DisplayName("SignupRequest: 8자 미만 비밀번호는 password 위반을 만든다")
    void signupRequest_shortPassword_hasViolation() {
        SignupRequest req = validSignup();
        req.setPassword("short"); // 8자 미만
        assertThat(validator.validate(req))
                .anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    @Test
    @DisplayName("SignupRequest: 필수값 누락 시 위반이 발생한다")
    void signupRequest_missingRequired_hasViolations() {
        SignupRequest req = new SignupRequest();
        assertThat(validator.validate(req)).isNotEmpty();
    }

    @Test
    @DisplayName("SignupRequest: 유효한 값은 위반이 없다")
    void signupRequest_valid_noViolations() {
        assertThat(validator.validate(validSignup())).isEmpty();
    }

    private SignupRequest validSignup() {
        SignupRequest req = new SignupRequest();
        req.setName("홍길동");
        req.setEmail("user@haru.com");
        req.setNickname("gildong");
        req.setPassword("password123");
        return req;
    }
}
