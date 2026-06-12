// http://localhost:8081 — 구현 해야하는 code서버 (백엔드 DB, redis, 웹소켓)

package com.example.demo.controller;

import com.example.demo.dto.response.*;
import com.example.demo.dto.auth.*;
import com.example.demo.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/core/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(@Valid @RequestBody SignupRequest signupRequest) {
        SignupResponse response = userService.registerUser(signupRequest);
        if (response.isSuccess()) {
            return ResponseEntity.ok(ApiResponse.success(response));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error(response, "400"));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
        LoginResponse response = userService.login(loginRequest);
        if (response.isSuccess()) {
            return ResponseEntity.ok(ApiResponse.success(response));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error(response, "400"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<LogoutResponse>> logout(@RequestHeader("Authorization") String token) {
        LogoutResponse response = userService.logout(token);
        if (response.isSuccess()) {
            return ResponseEntity.ok(ApiResponse.success(response));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error(response, "400"));
        }
    }

    /**
     * 회원 탈퇴
     */
    @PostMapping("/me/withdrawal")
    public ResponseEntity<ApiResponse<WithdrawalResponse>> withdraw(
            @RequestHeader("Authorization") String token,
            @RequestBody WithdrawalRequest request) {

        WithdrawalResponse response = userService.withdrawUserByToken(token, request);

        if (!response.isSuccess()) {
            return ResponseEntity.badRequest().body(ApiResponse.error(response, "400"));
        }

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 비밀번호 변경
     */
    // 로그인한 유저를 위한 비밀번호 변경(토큰 존재할때)
    @PutMapping("/me/password")
    public ResponseEntity<PasswordChangeResponse> changePassword(
            @RequestHeader("Authorization") String token,
            @RequestBody PasswordChangeRequest request) {
        // 서비스 호출
        PasswordChangeResponse response = userService.changePasswordByToken(token, request);

        if (!response.isSuccess()) {
            return ResponseEntity.badRequest().body(response);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * 이메일 존재 여부 검증
     */

    /**
     * 이메일과 전화번호 검증증
     */
}
