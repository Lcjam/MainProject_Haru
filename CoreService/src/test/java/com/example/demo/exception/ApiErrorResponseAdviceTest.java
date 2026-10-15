package com.example.demo.exception;

import com.example.demo.dto.response.ApiResponse;
import com.example.demo.util.BaseResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ApiErrorResponseAdviceTest {
    private final ApiErrorResponseAdvice advice = new ApiErrorResponseAdvice(new ObjectMapper());
    @Test
    void legacyNumericErrorAlignsHttp200AndReplacesInternalMessageAndData() {
        var servlet = new MockHttpServletResponse();
        var original = ApiResponse.error("SQL Exception at com.example.private", "500");
        var body = (ApiResponse<?>) advice.beforeBodyWrite(original, null,
                MediaType.APPLICATION_JSON, null, new ServletServerHttpRequest(new MockHttpServletRequest()),
                new ServletServerHttpResponse(servlet));
        assertEquals(500, servlet.getStatus());
        assertEquals(ApiErrorCode.INTERNAL_ERROR.name(), body.getCode());
        assertEquals(ApiErrorCode.INTERNAL_ERROR.message(), body.getMessage());
        assertEquals(body.getMessage(), body.getData());
    }

    @Test
    void nestedLegacyInternalDetailsAreRemoved() {
        var body = (ApiResponse<?>) advice.normalize(
                ApiResponse.error(Map.of("message", "SQL Exception private"), "500"), 500);
        assertNull(body.getData());
        assertEquals(ApiErrorCode.INTERNAL_ERROR.message(), body.getMessage());
    }

    @Test
    void baseResponseUsesHttpStatusEvenIfLegacyConstructorSaysSuccess() {
        var original = new BaseResponse<>(null, "SQL Exception private");
        var body = (ApiResponse<?>) advice.normalize(original, 500);
        assertEquals("error", body.getStatus());
        assertEquals(ApiErrorCode.INTERNAL_ERROR.name(), body.getCode());
        assertEquals(ApiErrorCode.INTERNAL_ERROR.message(), body.getMessage());
        var unavailable = (ApiResponse<?>) advice.normalize(new BaseResponse<>(null, "private host"), 502);
        assertEquals(ApiErrorCode.UPSTREAM_UNAVAILABLE.name(), unavailable.getCode());
        var badRequest = (ApiResponse<?>) advice.normalize(BaseResponse.error("위치 정보가 없습니다."), 200);
        assertEquals(ApiErrorCode.BAD_REQUEST.name(), badRequest.getCode());
        assertEquals("위치 정보가 없습니다.", badRequest.getMessage());
    }

    @Test
    void legacyMapMessageIsPromotedOnlyWhenTopLevelIsMissing() {
        var nested = Map.of("message", "이미 사용 중인 닉네임입니다.");
        var promoted = (ApiResponse<?>) advice.normalize(ApiResponse.error(nested, "409"), 409);
        assertEquals(nested.get("message"), promoted.getMessage());
        var explicit = (ApiResponse<?>) advice.normalize(
                new ApiResponse<>("error", nested, "409", "명시한 업무 문구"), 409);
        assertEquals("명시한 업무 문구", explicit.getMessage());
        for (Object data : new Object[]{Map.of(), Map.of("message", " "), Map.of("message", 123)}) {
            var fallback = (ApiResponse<?>) advice.normalize(ApiResponse.error(data, "400"), 400);
            assertEquals(ApiErrorCode.BAD_REQUEST.message(), fallback.getMessage());
        }
        var fixed = (ApiResponse<?>) advice.normalize(ApiResponse.error(nested, "405"), 405);
        assertEquals(ApiErrorCode.METHOD_NOT_ALLOWED.message(), fixed.getMessage());
    }

    @Test
    void successfulResponsesStayTheSameObject() {
        var api = ApiResponse.success("ok");
        var base = BaseResponse.success("ok");
        assertSame(api, advice.normalize(api, 200));
        assertSame(base, advice.normalize(base, 200));
    }
}
