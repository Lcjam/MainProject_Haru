package com.example.demo.security;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.example.demo.controller.LlamaController;
import com.example.demo.controller.CloudOCRController;
import com.example.demo.controller.SmsController;
import com.example.demo.dao.ChatMessageDAO;
import com.example.demo.dto.ChatRequestDTO;
import com.example.demo.serviceimpl.CloudOCRServiceImpl;
import com.example.demo.serviceimpl.LlamaServiceImpl;
import com.example.demo.serviceimpl.SmsService;
import com.example.demo.util.TokenUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SensitiveLogTest {

    @Test
    void otpVerificationDoesNotLogPhoneNumberOrCode() {
        SmsService smsService = mock(SmsService.class);
        when(smsService.verifyCode("01012345678", "9876")).thenReturn(true);
        SmsController controller = new SmsController();
        setField(controller, "smsService", smsService);
        ListAppender<ILoggingEvent> logs = capture(SmsController.class);

        controller.verifyOtp(Map.of("phoneNumber", "01012345678", "otp", "9876"));

        assertThat(messages(logs)).doesNotContain("01012345678", "9876");
    }

    @Test
    void ocrReturnsExtractedTextWithoutLoggingCardContents() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        String cardText = "4111111111111111";
        String body = "{\"images\":[{\"inferResult\":\"SUCCESS\",\"fields\":[{\"inferText\":\""
                + cardText + "\",\"inferConfidence\":0.99}]}]}";
        when(restTemplate.exchange(any(String.class), any(), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok(body));
        CloudOCRServiceImpl service = new CloudOCRServiceImpl(restTemplate, new ObjectMapper());
        setField(service, "secretKey", "test-secret");
        setField(service, "apiUrl", "https://ocr.example.test");
        ListAppender<ILoggingEvent> logs = capture(CloudOCRServiceImpl.class);

        String result = service.processImage(new MockMultipartFile(
                "file", "card.png", "image/png", "image".getBytes(StandardCharsets.UTF_8)));

        assertThat(result).isEqualTo(cardText);
        assertThat(messages(logs)).doesNotContain(cardText);
    }

    @Test
    void authenticationDoesNotLogAuthorizationToken() {
        TokenUtils tokenUtils = mock(TokenUtils.class);
        when(tokenUtils.getEmailFromToken("Bearer raw-secret-token"))
                .thenThrow(new IllegalArgumentException("raw-secret-token"));
        LlamaController controller = new LlamaController(
                mock(LlamaServiceImpl.class), mock(ChatMessageDAO.class), tokenUtils);
        ChatRequestDTO request = new ChatRequestDTO();
        request.setMessage("hello");
        ListAppender<ILoggingEvent> logs = capture(LlamaController.class);

        controller.chat(request, "Bearer raw-secret-token");

        assertThat(messages(logs)).doesNotContain("raw-secret-token");

        TokenUtils realTokenUtils = new TokenUtils("01234567890123456789012345678901");
        ListAppender<ILoggingEvent> tokenLogs = capture(TokenUtils.class);
        realTokenUtils.getEmailFromToken("Bearer raw-secret-token");
        assertThat(messages(tokenLogs)).doesNotContain("raw-secret-token");
    }

    @Test
    void ocrFailureDoesNotReturnProviderMessage() throws Exception {
        CloudOCRServiceImpl service = mock(CloudOCRServiceImpl.class);
        when(service.processImage(any())).thenThrow(new IllegalArgumentException("4111111111111111"));
        CloudOCRController controller = new CloudOCRController(service);

        var response = controller.processImage(new MockMultipartFile(
                "file", "card.png", "image/png", new byte[0]));

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(new ObjectMapper().writeValueAsString(response.getBody()))
                .doesNotContain("4111111111111111");
    }

    private static ListAppender<ILoggingEvent> capture(Class<?> type) {
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        ((Logger) LoggerFactory.getLogger(type)).addAppender(appender);
        return appender;
    }

    private static String messages(ListAppender<ILoggingEvent> appender) {
        return appender.list.stream().map(ILoggingEvent::getFormattedMessage).reduce("", String::concat);
    }

    private static void setField(Object target, String name, Object value) {
        try {
            var field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }
}
