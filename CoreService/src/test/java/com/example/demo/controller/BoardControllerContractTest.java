package com.example.demo.controller;

import com.example.demo.dto.response.ApiResponse;
import com.example.demo.exception.GlobalExceptionHandler;
import com.example.demo.exception.UnauthorizedException;
import com.example.demo.service.BoardService;
import com.example.demo.util.TokenUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * BoardController 의 HTTP 계약을 고정한다.
 * 단계 5b 에서 반복되던 {@code 401 가드 + try/catch(IllegalArgumentException→400, Exception→500)}
 * 보일러플레이트를 제거하고 {@link GlobalExceptionHandler} 위임으로 전환했다.
 * 이 테스트는 위임 후에도 상태코드/{@link ApiResponse} 형태(status·code·data)가 보존됨을 보장한다.
 * 보안 필터 슬라이스 얽힘을 피하기 위해 standalone MockMvc + Mockito + controllerAdvice 로 검증.
 */
@DisplayName("BoardController contract (GlobalExceptionHandler 위임)")
class BoardControllerContractTest {

    private final BoardService boardService = mock(BoardService.class);
    private final TokenUtils tokenUtils = mock(TokenUtils.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new BoardController(boardService, tokenUtils))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("토큰이 유효하지 않으면 401 + error/401 본문(인증되지 않은 요청입니다.)을 보존한다")
    void unauthorized_returns401WithPreservedBody() throws Exception {
        given(tokenUtils.requireEmail(anyString()))
                .willThrow(new UnauthorizedException("인증되지 않은 요청입니다."));

        mockMvc.perform(get("/api/core/boards/{boardId}", 1).header("Authorization", "Bearer bad"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value("401"))
                .andExpect(jsonPath("$.data").value("인증되지 않은 요청입니다."));
    }

    @Test
    @DisplayName("서비스가 IllegalArgumentException 을 던지면 400 + error/400 + 메시지로 위임된다")
    void illegalArgument_returns400ViaHandler() throws Exception {
        given(tokenUtils.requireEmail(anyString())).willReturn("user@haru.com");
        given(boardService.getBoardById(anyLong(), anyString()))
                .willThrow(new IllegalArgumentException("존재하지 않는 게시판입니다."));

        mockMvc.perform(get("/api/core/boards/{boardId}", 999).header("Authorization", "Bearer ok"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value("400"))
                .andExpect(jsonPath("$.data").value("존재하지 않는 게시판입니다."));
    }

    @Test
    @DisplayName("정상 조회는 200 + success 래퍼를 반환한다")
    void success_returns200() throws Exception {
        given(tokenUtils.requireEmail(anyString())).willReturn("user@haru.com");
        given(boardService.getBoardsByHost("user@haru.com")).willReturn(List.of());

        mockMvc.perform(get("/api/core/boards/hosted").header("Authorization", "Bearer ok"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.code").value("200"));
    }

    @Test
    @DisplayName("인증 후 인라인 전제검증 실패(status 누락)는 컨트롤러가 직접 400 을 반환한다")
    void inlinePrecondition_returns400() throws Exception {
        given(tokenUtils.requireEmail(anyString())).willReturn("user@haru.com");

        mockMvc.perform(put("/api/core/boards/{boardId}/status", 1)
                        .header("Authorization", "Bearer ok")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value("400"))
                .andExpect(jsonPath("$.data").value("상태 값이 필요합니다."));
    }
}
