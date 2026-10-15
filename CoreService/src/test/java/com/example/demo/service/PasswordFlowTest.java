package com.example.demo.service;

import com.example.demo.config.SecurityConfig;
import com.example.demo.dto.auth.*;
import com.example.demo.mapper.UserMapper;
import com.example.demo.model.User;
import com.example.demo.security.JwtTokenBlacklistService;
import com.example.demo.security.JwtTokenProvider;
import com.example.demo.util.TokenUtils;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PasswordFlowTest {
    private final UserMapper users = mock(UserMapper.class);
    private final PasswordEncoder passwords = new SecurityConfig(null, null, null, null).passwordEncoder();
    private final JwtTokenProvider tokens = mock(JwtTokenProvider.class);
    private final AuthService auth = new AuthService(users, passwords, tokens,
            mock(JwtTokenBlacklistService.class), mock(TokenUtils.class), mock(HobbyService.class));

    @Test
    void signupStoresBcryptWithTheExistingCostAndLoginAcceptsThatHash() {
        when(users.insertUser(any())).thenReturn(1);
        var signup = new SignupRequest();
        signup.setEmail("user@example.test");
        signup.setPassword("original-password");
        signup.setNickname("user");
        assertTrue(auth.registerUser(signup).isSuccess());
        var saved = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(users).insertUser(saved.capture());
        var user = saved.getValue();
        assertTrue(user.getPasswordHash().startsWith("$2a$12$"));
        user.setId(1L);
        when(users.findByEmail(user.getEmail())).thenReturn(user);
        when(tokens.createToken(anyInt(), anyString(), anyList())).thenReturn("jwt");

        var login = new LoginRequest();
        login.setEmail(user.getEmail());
        login.setPassword("original-password");
        assertTrue(auth.login(login).isSuccess());
        login.setPassword("wrong-password");
        assertFalse(auth.login(login).isSuccess());
    }

    @Test
    void passwordChangeAndWithdrawalAcceptHashesFromTheRemovedUtility() {
        // PasswordUtils used BCryptPasswordEncoder(12); existing database hashes must still work.
        var user = User.builder().email("user@example.test").loginMethod("EMAIL")
                .passwordHash(new BCryptPasswordEncoder(12).encode("old-password")).build();
        when(users.findByEmail(user.getEmail())).thenReturn(user);
        var profile = new ProfileService(users, mock(HobbyService.class), passwords,
                mock(TokenUtils.class), mock(ProfileImageService.class));
        var change = PasswordChangeRequest.builder().isToken("true").currentPassword("old-password")
                .newPassword("new-password").confirmPassword("new-password").build();
        assertTrue(profile.changePassword(user.getEmail(), change).isSuccess());
        var newHash = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(users).updateUserPasswordHash(eq(user.getEmail()), newHash.capture());
        assertTrue(newHash.getValue().startsWith("$2a$12$"));
        assertTrue(passwords.matches("new-password", newHash.getValue()));
        user.setPasswordHash(newHash.getValue());
        var withdrawal = new WithdrawalService(users, mock(TokenUtils.class), passwords,
                mock(JwtTokenBlacklistService.class), tokens, mock(FileStorageService.class));
        assertFalse(withdrawal.withdrawUser(user.getEmail(), WithdrawalRequest.builder().password("old-password").build()).isSuccess());
        assertTrue(withdrawal.withdrawUser(user.getEmail(), WithdrawalRequest.builder().password("new-password").build()).isSuccess());
    }
}
