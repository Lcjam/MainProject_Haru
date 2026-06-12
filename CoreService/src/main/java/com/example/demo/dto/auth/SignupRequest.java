package com.example.demo.dto.auth;

import com.example.demo.dto.hobby.HobbyRequest;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class SignupRequest {
    @NotBlank(message = "이름은 필수입니다.")
    private String name;

    private String phoneNumber;

    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;

    @NotBlank(message = "닉네임은 필수입니다.")
    private String nickname;

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 8, max = 64, message = "비밀번호는 8자 이상이어야 합니다.")
    private String password;

    private String bio;
    private String loginMethod;
    private String socialProvider;
    private List<HobbyRequest> hobbies;
}
