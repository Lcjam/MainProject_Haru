package com.example.demo.dto.auth;

import com.example.demo.dto.hobby.HobbyRequest;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class SignupRequest {
    private String name;
    private String phoneNumber;
    private String email;
    private String nickname;
    private String password;
    private String bio;
    private String loginMethod;
    private String socialProvider;
    private List<HobbyRequest> hobbies;
}
