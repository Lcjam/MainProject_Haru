package com.example.demo.serviceimpl;

import com.example.demo.dao.ChatMessageDAO;
import com.example.demo.model.ChatMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class ChatHttpContractTest {
    private final ObjectMapper json = new ObjectMapper();
    private final RestTemplate http = new RestTemplate();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(http).build();
    private final ChatMessageDAO messages = mock(ChatMessageDAO.class);
    private final CloudChatBotServiceImpl cloud = mock(CloudChatBotServiceImpl.class);
    private final LlamaServiceImpl llama = new LlamaServiceImpl(cloud, messages, json, http);

    @Test
    void cloudSignsTheExactUtf8JsonSentAndExtractsTheResponse() throws Exception {
        var service = new CloudChatBotServiceImpl(http, json);
        ReflectionTestUtils.setField(service, "secretKey", "test-secret");
        ReflectionTestUtils.setField(service, "apiUrl", "https://chat.example.test");
        server.expect(requestTo("https://chat.example.test"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(request -> {
                    byte[] body = ((MockClientHttpRequest) request).getBodyAsBytes();
                    var payload = json.readTree(body);
                    assertEquals("v2", payload.path("version").asText());
                    assertEquals("send", payload.path("event").asText());
                    assertTrue(payload.path("userId").asText().startsWith("user-"));
                    assertTrue(payload.path("timestamp").asLong() > 0);
                    assertEquals("text", payload.path("bubbles").path(0).path("type").asText());
                    assertEquals("한글 \"질문\"\n", payload.path("bubbles").path(0).path("data").path("description").asText());
                    try {
                        Mac mac = Mac.getInstance("HmacSHA256");
                        mac.init(new SecretKeySpec("test-secret".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
                        assertEquals(Base64.getEncoder().encodeToString(mac.doFinal(body)),
                                request.getHeaders().getFirst("X-NCP-CHATBOT_SIGNATURE"));
                    } catch (Exception e) {
                        throw new AssertionError(e);
                    }
                })
                .andRespond(withSuccess("{\"bubbles\":[{\"data\":{\"description\":\"답변\"}}]}", MediaType.APPLICATION_JSON));

        assertEquals("답변", service.getResponse("한글 \"질문\"\n"));
        server.verify();
    }

    @Test
    void successfulCloudResponseSkipsTranslationAndSavesBothMessages() {
        when(cloud.getResponse("질문")).thenReturn(" 답변 ");

        assertEquals("답변", llama.chat("질문", "user@example.test", List.of()));
        var saved = org.mockito.ArgumentCaptor.forClass(ChatMessage.class);
        verify(messages, times(2)).saveMessage(saved.capture());
        assertEquals(List.of("user", "assistant"), saved.getAllValues().stream().map(ChatMessage::getMessageType).toList());
        assertEquals(List.of("질문", "답변"), saved.getAllValues().stream().map(ChatMessage::getContent).toList());
        assertTrue(saved.getAllValues().stream().allMatch(m -> "user@example.test".equals(m.getSessionId())));
        server.verify();
    }

    @Test
    void fallbackEncodesQueryValuesAndSendsPairedHistoryAsJson() {
        ReflectionTestUtils.setField(llama, "gatewayUri", "http://gateway.test");
        ReflectionTestUtils.setField(llama, "translateApiKey", "key&+");
        ReflectionTestUtils.setField(llama, "translateApiEmail", "dev+chat@example.test");
        when(cloud.getResponse(any())).thenThrow(new IllegalStateException("offline"));
        expectTranslation("%ED%95%9C%EA%B8%80%20%26%20%2B%3F", "ko%7Cen", "question");
        expectTranslation("old%20question", "ko%7Cen", "previous question");
        expectTranslation("old%20answer", "ko%7Cen", "previous answer");
        expectTranslation("last%20question", "ko%7Cen", "last question");
        server.expect(requestTo("http://gateway.test/api/fastapi/chat"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {"message":"question","sessionId":"user@example.test","history":[
                          {"user":"previous question","assistant":"previous answer"}, {"user":"last question"}]}
                        """))
                .andRespond(withSuccess("{\"response\":\"English answer\"}", MediaType.APPLICATION_JSON));
        expectTranslation("English%20answer", "en%7Cko", "한글 답변");
        var history = List.of(message("old question"), message("old answer"), message("last question"));

        assertEquals("한글 답변", llama.chat("한글 & +?", "user@example.test", history));
        verify(messages, times(2)).saveMessage(any());
        server.verify();
    }

    @Test
    void gatewayHttpErrorUsesTheExistingFallbackResponse() {
        ReflectionTestUtils.setField(llama, "gatewayUri", "http://gateway.test");
        when(cloud.getResponse(any())).thenReturn("false");
        server.expect(requestTo("https://api.mymemory.translated.net/get?q=question&langpair=ko%7Cen"))
                .andRespond(withSuccess("{\"responseData\":{\"translatedText\":\"question\"}}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://gateway.test/api/fastapi/chat"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        server.expect(requestTo("https://api.mymemory.translated.net/get?q=An%20error%20occurred%20while%20processing%20your%20request.&langpair=en%7Cko"))
                .andRespond(withSuccess("{\"responseData\":{\"translatedText\":\"오류 안내\"}}", MediaType.APPLICATION_JSON));

        assertEquals("오류 안내", llama.chat("question", "user@example.test", List.of()));
        server.verify();
    }

    private void expectTranslation(String encodedText, String languages, String translation) {
        server.expect(requestTo("https://api.mymemory.translated.net/get?q=" + encodedText
                        + "&langpair=" + languages + "&key=key%26%2B&de=dev%2Bchat%40example.test"))
                .andRespond(withSuccess("{\"responseData\":{\"translatedText\":\"" + translation + "\"}}", MediaType.APPLICATION_JSON));
    }

    private ChatMessage message(String content) {
        var message = new ChatMessage();
        message.setContent(content);
        return message;
    }
}
