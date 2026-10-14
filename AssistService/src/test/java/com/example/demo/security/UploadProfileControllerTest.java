package com.example.demo.security;

import com.example.demo.controller.UploadProfileController;
import com.example.demo.util.TokenUtils;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UploadProfileControllerTest {
    private final TokenUtils tokens = mock(TokenUtils.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new UploadProfileController(tokens)).build();
    private final MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[]{1});

    @Test
    void unauthenticatedUploadIsRejected() throws Exception {
        when(tokens.authenticationErrorCode(null)).thenReturn("AUTH_REQUIRED");
        mvc.perform(multipart("/api/assist/upload/profile").file(file))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
    }

    @Test
    void authenticatedRequestCannotReceiveFalseStorageSuccess() throws Exception {
        mvc.perform(multipart("/api/assist/upload/profile").file(file)
                        .header("Authorization", "Bearer valid"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.data.response").value("/api/core/profiles/me/image"));
    }
}
