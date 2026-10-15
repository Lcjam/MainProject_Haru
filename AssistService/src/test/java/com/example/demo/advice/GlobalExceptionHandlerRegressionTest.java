package com.example.demo.advice;

import com.example.demo.controller.CustomErrorController;
import com.example.demo.controller.EndLocationController;
import com.example.demo.controller.UploadProfileController;
import com.example.demo.util.TokenUtils;
import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.dto.CommonResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerRegressionTest {

    private MockMvc mockMvc;
    private TokenUtils tokenUtils;

    @RestController
    static class HelperTestController {
        @PostMapping(value = "/api/assist/test/json-only", consumes = MediaType.APPLICATION_JSON_VALUE)
        public String testJsonOnly(@RequestBody String body) {
            return body;
        }

        @PostMapping("/api/assist/test/upload-limit")
        public void throwMaxUploadSizeExceeded() {
            throw new MaxUploadSizeExceededException(20 * 1024 * 1024L);
        }

        @GetMapping("/api/assist/test/response-status")
        public void throwResponseStatus() {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "중복 상태 발생");
        }

        @GetMapping("/api/assist/test/illegal-argument-marker")
        public void throwIllegalArgumentMarker() {
            throw new IllegalArgumentException("provider-body=INTERNAL_MARKER");
        }

        @GetMapping("/api/assist/test/response-status-marker")
        public void throwResponseStatusMarker() {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "INTERNAL_REASON_MARKER");
        }

        @PostMapping("/api/assist/test/validation")
        public void throwValidationException() throws Exception {
            Method method = HelperTestController.class.getMethod("throwValidationException");
            MethodParameter parameter = new MethodParameter(method, -1);
            BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "dummy");
            bindingResult.addError(new FieldError("dummy", "name", "이름은 필수입니다."));
            throw new MethodArgumentNotValidException(parameter, bindingResult);
        }

        @GetMapping("/api/assist/test/header")
        public String testHeader(@RequestHeader("X-Required-Header") String header) {
            return header;
        }
    }

    @BeforeEach
    void setUp() {
        tokenUtils = mock(TokenUtils.class);
        when(tokenUtils.authenticationErrorCode(anyString())).thenReturn(null);

        EndLocationController endLocationController = new EndLocationController(new RestTemplate());
        UploadProfileController uploadProfileController = new UploadProfileController(tokenUtils);
        CustomErrorController customErrorController = new CustomErrorController();
        HelperTestController helperTestController = new HelperTestController();

        mockMvc = MockMvcBuilders.standaloneSetup(
                        endLocationController,
                        uploadProfileController,
                        customErrorController,
                        helperTestController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("1. EndLocationController query 누락 시 400 BAD_REQUEST 반환 (MissingServletRequestParameterException)")
    void testEndLocationMissingQueryParameter_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/assist/location/search"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."));
    }

    @Test
    @DisplayName("2. EndLocationController lat 타입 불일치 시 400 BAD_REQUEST 반환 (MethodArgumentTypeMismatchException)")
    void testEndLocationCoordToAddressTypeMismatch_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/assist/location/coord-to-address")
                        .param("lat", "abc")
                        .param("lng", "127.0"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."));
    }

    @Test
    @DisplayName("3. UploadProfileController file 파트 누락 시 400 BAD_REQUEST 반환 (MissingServletRequestPartException)")
    void testUploadProfileMissingFilePart_returnsBadRequest() throws Exception {
        mockMvc.perform(multipart("/api/assist/upload/profile")
                        .file(new MockMultipartFile("wrongPart", "wrong.png", "image/png", new byte[]{1}))
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."));
    }

    @Test
    @DisplayName("4. 업로드 20MB 초과 시 413 PAYLOAD_TOO_LARGE 반환 (MaxUploadSizeExceededException)")
    void testUploadExceededLimit_returnsPayloadTooLarge() throws Exception {
        mockMvc.perform(post("/api/assist/test/upload-limit"))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value("PAYLOAD_TOO_LARGE"))
                .andExpect(jsonPath("$.message").value("요청 크기가 너무 큽니다."));
    }

    @Test
    @DisplayName("5. 잘못된 Content-Type 요청 시 415 UNSUPPORTED_MEDIA_TYPE 반환 (HttpMediaTypeNotSupportedException)")
    void testUnsupportedMediaType_returnsUnsupportedMediaType() throws Exception {
        mockMvc.perform(post("/api/assist/test/json-only")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("hello plain text"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"))
                .andExpect(jsonPath("$.message").value("지원하지 않는 요청 형식입니다."));
    }

    @Test
    @DisplayName("6. CustomErrorController Accept: text/html 요청 시에도 계약 JSON 반환 (404 NOT_FOUND)")
    void testCustomErrorControllerHtmlAcceptHeader_returnsJsonEnvelope() throws Exception {
        mockMvc.perform(get("/error")
                        .accept(MediaType.TEXT_HTML)
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 404))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("요청한 자원을 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("7. CustomErrorController 500 에러 시 계약 JSON 반환 (INTERNAL_ERROR, 고정 문구)")
    void testCustomErrorController500_returnsJsonInternalError() throws Exception {
        mockMvc.perform(get("/error")
                        .accept(MediaType.TEXT_HTML)
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 500))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("서버 오류가 발생했습니다."));
    }

    @Test
    @DisplayName("8. MethodArgumentNotValidException 발생 시 400 BAD_REQUEST 및 필드 에러 메시지 반환")
    void testValidationFailure_returnsBadRequestWithMessage() throws Exception {
        mockMvc.perform(post("/api/assist/test/validation"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("이름은 필수입니다."));
    }

    @Test
    @DisplayName("9. MissingRequestHeaderException 발생 시 400 BAD_REQUEST 반환")
    void testMissingHeader_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/assist/test/header"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."));
    }

    @Test
    @DisplayName("10. ResponseStatusException 발생 시 상태 코드에 매핑된 계약 반환 (409 CONFLICT) 및 기본 문구 사용")
    void testResponseStatusException_mapsToContractCode() throws Exception {
        mockMvc.perform(get("/api/assist/test/response-status"))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("요청이 현재 상태와 충돌합니다."));
    }

    @Test
    @DisplayName("11. IllegalArgumentException 내부 마커가 응답 본문에 노출되지 않아야 함 (P2 회귀)")
    void testIllegalArgumentMarker_doesNotLeakInternalMarker() throws Exception {
        mockMvc.perform(get("/api/assist/test/illegal-argument-marker"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
                .andExpect(content().string(not(containsString("INTERNAL_MARKER"))));
    }

    @Test
    @DisplayName("12. ResponseStatusException reason 마커가 응답 본문에 노출되지 않아야 함 (P2 회귀)")
    void testResponseStatusMarker_doesNotLeakInternalMarker() throws Exception {
        mockMvc.perform(get("/api/assist/test/response-status-marker"))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("요청이 현재 상태와 충돌합니다."))
                .andExpect(content().string(not(containsString("INTERNAL_REASON_MARKER"))));
    }

    @Test
    @DisplayName("13. 405 Method Not Allowed 시 Allow 헤더가 유지되어야 함 (P3 회귀)")
    void testMethodNotAllowed_preservesAllowHeader() throws Exception {
        mockMvc.perform(get("/api/assist/test/upload-limit"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", "POST"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"))
                .andExpect(jsonPath("$.message").value("허용되지 않은 요청 방식입니다."));
    }

    @Test
    @DisplayName("14. CommonResponseDTO 성공 직렬화 시 message:null이 포함되어야 함 (P3 회귀)")
    void testCommonResponseDTO_serializationPreservesNullMessageOnSuccess() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        CommonResponseDTO<String> successDto = CommonResponseDTO.<String>builder()
                .status("success")
                .data(new CommonResponseDTO.Data<>("성공적으로 이미지 처리 완료", "ocr-result"))
                .code("200")
                .build();

        String json = mapper.writeValueAsString(successDto);
        assertThat(json).contains("\"message\":null");
        assertThat(json).contains("\"status\":\"success\"");
        assertThat(json).contains("\"code\":\"200\"");
    }
}
