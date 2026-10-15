package com.example.demo.serviceimpl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class CloudOCRServiceTest {
    @Test
    void sendsTheExistingOcrContractAndExtractsOnlySuccessfulImageFields() {
        var http = new RestTemplate();
        var json = new ObjectMapper();
        var server = MockRestServiceServer.bindTo(http).build();
        var service = new CloudOCRServiceImpl(http, json);
        ReflectionTestUtils.setField(service, "apiUrl", "https://ocr.example.test");
        ReflectionTestUtils.setField(service, "secretKey", "secret");
        server.expect(requestTo("https://ocr.example.test")).andExpect(header("X-OCR-SECRET", "secret"))
                .andExpect(request -> {
                    var payload = json.readTree(((MockClientHttpRequest) request).getBodyAsBytes());
                    assertEquals("V2", payload.path("version").asText());
                    assertEquals("ko", payload.path("lang").asText());
                    var image = payload.path("images").path(0);
                    assertEquals("png", image.path("format").asText());
                    assertEquals("AQID", image.path("data").asText());
                    assertEquals(36479, image.path("templateIds").path(0).asInt());
                })
                .andRespond(withSuccess("""
                        {"images":[
                          {"inferResult":"FAILURE","fields":[{"inferText":"skip"}]},
                          {"inferResult":"SUCCESS","fields":[{"inferText":"첫째"},{"inferText":"둘째"}]},
                          {"inferResult":"SUCCESS","fields":[]}]}
                        """, MediaType.APPLICATION_JSON));
        var image = new MockMultipartFile("file", "card.png", "image/png", new byte[]{1, 2, 3});
        assertEquals("첫째 둘째", service.processImage(image));
        assertThrows(RuntimeException.class, () -> service.processImage(
                new MockMultipartFile("file", "card.svg", "image/svg+xml", new byte[]{1})));
        server.verify();
    }
}
