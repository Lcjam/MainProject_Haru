package com.example.demo.security;

import com.example.demo.controller.CloudOCRController;
import com.example.demo.filter.AssistAuthenticationFilter;
import com.example.demo.serviceimpl.CloudOCRServiceImpl;
import com.example.demo.util.TokenUtils;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CloudOcrAuthSecurityTest {

    private final CloudOCRServiceImpl cloudOCRService = mock(CloudOCRServiceImpl.class);
    private final TokenUtils tokenUtils = new TokenUtils("test-secret-key-at-least-32-bytes-long");
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new CloudOCRController(cloudOCRService))
            .addFilters(new AssistAuthenticationFilter(tokenUtils))
            .build();

    @Test
    void blankAuthorizationHeaderBlocksOcrAndNeverCallsService() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/assist/cloudocr/process")
                        .file(file)
                        .header("Authorization", ""))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));

        mockMvc.perform(multipart("/api/assist/cloudocr/process")
                        .file(file)
                        .header("Authorization", "   "))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));

        verifyNoInteractions(cloudOCRService);
    }
}
