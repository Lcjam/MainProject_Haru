package com.example.demo.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class EndLocationControllerTest {
    @Test
    void encodesSearchValuesAndPreservesCoordinateAndErrorResponses() {
        var http = new RestTemplate();
        var server = MockRestServiceServer.bindTo(http).build();
        var controller = new EndLocationController(http);
        ReflectionTestUtils.setField(controller, "kakaoRestApiKey", "key");
        server.expect(requestTo("https://dapi.kakao.com/v2/local/search/keyword.json?query=%EC%84%9C%EC%9A%B8%20%26%20%2B&size=5"))
                .andExpect(header("Authorization", "KakaoAK key"))
                .andRespond(withSuccess("{\"documents\":[]}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://dapi.kakao.com/v2/local/geo/coord2address.json?x=127.0&y=37.5&input_coord=WGS84"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("invalid key"));
        server.expect(requestTo("https://dapi.kakao.com/v2/local/search/keyword.json?query=invalid&size=5"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("invalid key"));
        var response = controller.searchAddress("서울 & +");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
        assertEquals("{\"documents\":[]}", response.getBody());
        assertEquals(HttpStatus.UNAUTHORIZED, controller.coordToAddress(37.5, 127.0).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, controller.searchAddress("invalid").getStatusCode());
        server.verify();
    }
}
