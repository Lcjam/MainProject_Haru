package com.example.demo.controller.board;

import com.example.demo.dto.board.PostReactionResponse;
import com.example.demo.exception.GlobalExceptionHandler;
import com.example.demo.model.board.PostReaction;
import com.example.demo.service.PostReactionService;
import com.example.demo.util.TokenUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PostReactionController 의 현행 HTTP 계약을 고정한다 (Phase 1a — Mapper 직접 주입 → 서비스 이동 전 안전망).
 * Mapper 상호작용은 검증하지 않는다 — 이동 후에도 그대로 그린이어야 하므로 상태코드/ApiResponse 형태만 단언한다.
 *
 * 주의: 500 catch 경로 중 컨트롤러가 {@code e.getMessage()} 를 응답 본문에 직접 이어붙이는 경로는
 * 상태코드/code 필드만 단언하고 본문 문자열은 단언하지 않는다(후속 단위에서 정보 노출 제거 예정 —
 * 스폰 지시서는 addReaction/deleteReaction 2곳만 언급했으나, toggleLike 도 동일 패턴이라 함께 적용했다.
 * 최종 보고에 기재).
 */
@DisplayName("PostReactionController contract")
class PostReactionControllerContractTest {

    private final PostReactionService postReactionService = mock(PostReactionService.class);
    private final TokenUtils tokenUtils = mock(TokenUtils.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new PostReactionController(postReactionService, tokenUtils))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ---- POST /api/core/boards/posts/{postId}/reactions (addReaction) ----

    @Test
    @DisplayName("addReaction: 이메일 null이면 401")
    void addReaction_unauthorized() throws Exception {
        given(tokenUtils.getEmailFromAuthHeader(anyString())).willReturn(null);

        mockMvc.perform(post("/api/core/boards/posts/{postId}/reactions", 1)
                        .header("Authorization", "Bearer bad")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reactionType\":\"LIKE\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("401"))
                .andExpect(jsonPath("$.data").value("인증되지 않은 요청입니다."));
    }

    @Test
    @DisplayName("addReaction: reactionType 누락이면 400")
    void addReaction_missingReactionType_returns400() throws Exception {
        given(tokenUtils.getEmailFromAuthHeader(anyString())).willReturn("user@haru.com");

        mockMvc.perform(post("/api/core/boards/posts/{postId}/reactions", 1)
                        .header("Authorization", "Bearer ok")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("400"))
                .andExpect(jsonPath("$.data").value("반응 타입은 필수입니다."));
    }

    @Test
    @DisplayName("addReaction: 새 반응 추가는 200 + success")
    void addReaction_success() throws Exception {
        given(tokenUtils.getEmailFromAuthHeader(anyString())).willReturn("user@haru.com");
        given(postReactionService.applyReaction(1L, "user@haru.com", "LIKE")).willReturn(Map.of("LIKE", 1));

        mockMvc.perform(post("/api/core/boards/posts/{postId}/reactions", 1)
                        .header("Authorization", "Bearer ok")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reactionType\":\"LIKE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.reactionType").value("LIKE"));
    }

    @Test
    @DisplayName("addReaction: 예외 발생 시 500(본문 문자열은 단언하지 않음)")
    void addReaction_exception_returns500() throws Exception {
        given(tokenUtils.getEmailFromAuthHeader(anyString())).willReturn("user@haru.com");
        given(postReactionService.applyReaction(anyLong(), anyString(), anyString()))
                .willThrow(new RuntimeException("DB 오류"));

        mockMvc.perform(post("/api/core/boards/posts/{postId}/reactions", 1)
                        .header("Authorization", "Bearer ok")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reactionType\":\"LIKE\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("500"));
    }

    // ---- DELETE /api/core/boards/posts/{postId}/reactions (deleteReaction) ----

    @Test
    @DisplayName("deleteReaction: 이메일 null이면 401")
    void deleteReaction_unauthorized() throws Exception {
        given(tokenUtils.getEmailFromAuthHeader(anyString())).willReturn(null);

        mockMvc.perform(delete("/api/core/boards/posts/{postId}/reactions", 1)
                        .header("Authorization", "Bearer bad"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("401"));
    }

    @Test
    @DisplayName("deleteReaction: 삭제할 반응이 없으면 404")
    void deleteReaction_notFound_returns404() throws Exception {
        given(tokenUtils.getEmailFromAuthHeader(anyString())).willReturn("user@haru.com");
        given(postReactionService.getUserReaction(1L, "user@haru.com")).willReturn(null);

        mockMvc.perform(delete("/api/core/boards/posts/{postId}/reactions", 1)
                        .header("Authorization", "Bearer ok"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("404"))
                .andExpect(jsonPath("$.data").value("삭제할 반응이 없습니다."));
    }

    @Test
    @DisplayName("deleteReaction: 정상 삭제는 200 + success")
    void deleteReaction_success() throws Exception {
        given(tokenUtils.getEmailFromAuthHeader(anyString())).willReturn("user@haru.com");
        PostReaction reaction = PostReaction.builder().postId(1L).userEmail("user@haru.com")
                .reactionType("LIKE").build();
        given(postReactionService.getUserReaction(1L, "user@haru.com")).willReturn(reaction);
        given(postReactionService.deleteReactionAndSync(1L, "user@haru.com", "LIKE")).willReturn(Map.of());

        mockMvc.perform(delete("/api/core/boards/posts/{postId}/reactions", 1)
                        .header("Authorization", "Bearer ok"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.message").value("반응이 삭제되었습니다."));
    }

    @Test
    @DisplayName("deleteReaction: 예외 발생 시 500(본문 문자열은 단언하지 않음)")
    void deleteReaction_exception_returns500() throws Exception {
        given(tokenUtils.getEmailFromAuthHeader(anyString())).willReturn("user@haru.com");
        given(postReactionService.getUserReaction(anyLong(), anyString()))
                .willThrow(new RuntimeException("DB 오류"));

        mockMvc.perform(delete("/api/core/boards/posts/{postId}/reactions", 1)
                        .header("Authorization", "Bearer ok"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("500"));
    }

    // ---- GET /api/core/boards/posts/{postId}/reactions (getPostReaction) ----

    @Test
    @DisplayName("getPostReaction: 이메일 null이면 401")
    void getPostReaction_unauthorized() throws Exception {
        given(tokenUtils.getEmailFromAuthHeader(anyString())).willReturn(null);

        mockMvc.perform(get("/api/core/boards/posts/{postId}/reactions", 1)
                        .header("Authorization", "Bearer bad"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("401"));
    }

    @Test
    @DisplayName("getPostReaction: IllegalArgumentException이면 400 + 메시지")
    void getPostReaction_illegalArgument_returns400() throws Exception {
        given(tokenUtils.getEmailFromAuthHeader(anyString())).willReturn("user@haru.com");
        given(postReactionService.getPostReaction("user@haru.com", 1L))
                .willThrow(new IllegalArgumentException("존재하지 않는 게시글입니다."));

        mockMvc.perform(get("/api/core/boards/posts/{postId}/reactions", 1)
                        .header("Authorization", "Bearer ok"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("400"))
                .andExpect(jsonPath("$.data").value("존재하지 않는 게시글입니다."));
    }

    @Test
    @DisplayName("getPostReaction: 정상 조회는 200 + success")
    void getPostReaction_success() throws Exception {
        given(tokenUtils.getEmailFromAuthHeader(anyString())).willReturn("user@haru.com");
        given(postReactionService.getPostReaction("user@haru.com", 1L))
                .willReturn(PostReactionResponse.builder().postId(1L).build());

        mockMvc.perform(get("/api/core/boards/posts/{postId}/reactions", 1)
                        .header("Authorization", "Bearer ok"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.code").value("200"));
    }

    @Test
    @DisplayName("getPostReaction: 일반 예외면 500 + 고정 메시지")
    void getPostReaction_genericException_returns500() throws Exception {
        given(tokenUtils.getEmailFromAuthHeader(anyString())).willReturn("user@haru.com");
        given(postReactionService.getPostReaction(anyString(), anyLong()))
                .willThrow(new RuntimeException("DB 오류"));

        mockMvc.perform(get("/api/core/boards/posts/{postId}/reactions", 1)
                        .header("Authorization", "Bearer ok"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("500"))
                .andExpect(jsonPath("$.data").value("서버 오류가 발생했습니다."));
    }

    // ---- GET /api/core/boards/posts/{postId}/reactions/list (getPostReactions) ----

    @Test
    @DisplayName("getPostReactions: 이메일 null이면 401")
    void getPostReactions_unauthorized() throws Exception {
        given(tokenUtils.getEmailFromAuthHeader(anyString())).willReturn(null);

        mockMvc.perform(get("/api/core/boards/posts/{postId}/reactions/list", 1)
                        .header("Authorization", "Bearer bad"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("401"));
    }

    @Test
    @DisplayName("getPostReactions: IllegalArgumentException이면 400 + 메시지")
    void getPostReactions_illegalArgument_returns400() throws Exception {
        given(tokenUtils.getEmailFromAuthHeader(anyString())).willReturn("user@haru.com");
        given(postReactionService.getPostReactions("user@haru.com", 1L))
                .willThrow(new IllegalArgumentException("존재하지 않는 게시글입니다."));

        mockMvc.perform(get("/api/core/boards/posts/{postId}/reactions/list", 1)
                        .header("Authorization", "Bearer ok"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("400"))
                .andExpect(jsonPath("$.data").value("존재하지 않는 게시글입니다."));
    }

    @Test
    @DisplayName("getPostReactions: 정상 조회는 200 + success")
    void getPostReactions_success() throws Exception {
        given(tokenUtils.getEmailFromAuthHeader(anyString())).willReturn("user@haru.com");
        given(postReactionService.getPostReactions("user@haru.com", 1L)).willReturn(List.of());

        mockMvc.perform(get("/api/core/boards/posts/{postId}/reactions/list", 1)
                        .header("Authorization", "Bearer ok"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.code").value("200"));
    }

    @Test
    @DisplayName("getPostReactions: 일반 예외면 500 + 고정 메시지")
    void getPostReactions_genericException_returns500() throws Exception {
        given(tokenUtils.getEmailFromAuthHeader(anyString())).willReturn("user@haru.com");
        given(postReactionService.getPostReactions(anyString(), anyLong()))
                .willThrow(new RuntimeException("DB 오류"));

        mockMvc.perform(get("/api/core/boards/posts/{postId}/reactions/list", 1)
                        .header("Authorization", "Bearer ok"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("500"))
                .andExpect(jsonPath("$.data").value("서버 오류가 발생했습니다."));
    }

    // ---- POST /api/core/boards/{postId}/like (toggleLike) ----

    @Test
    @DisplayName("toggleLike: 이메일 null이면 401")
    void toggleLike_unauthorized() throws Exception {
        given(tokenUtils.getEmailFromAuthHeader(anyString())).willReturn(null);

        mockMvc.perform(post("/api/core/boards/{postId}/like", 1)
                        .header("Authorization", "Bearer bad"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("401"));
    }

    @Test
    @DisplayName("toggleLike: 미반응 상태에서 호출하면 좋아요 추가 + 200")
    void toggleLike_addsLike_returns200() throws Exception {
        given(tokenUtils.getEmailFromAuthHeader(anyString())).willReturn("user@haru.com");
        Map<String, Object> serviceResult = new HashMap<>();
        serviceResult.put("liked", true);
        serviceResult.put("message", "좋아요가 추가되었습니다.");
        serviceResult.put("likeCount", 5);
        given(postReactionService.togglePostLike(1L, "user@haru.com")).willReturn(serviceResult);

        mockMvc.perform(post("/api/core/boards/{postId}/like", 1)
                        .header("Authorization", "Bearer ok"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.liked").value(true))
                .andExpect(jsonPath("$.data.likeCount").value(5));
    }

    @Test
    @DisplayName("toggleLike: 이미 반응한 상태에서 호출하면 좋아요 취소 + 200")
    void toggleLike_removesLike_returns200() throws Exception {
        given(tokenUtils.getEmailFromAuthHeader(anyString())).willReturn("user@haru.com");
        Map<String, Object> serviceResult = new HashMap<>();
        serviceResult.put("liked", false);
        serviceResult.put("message", "좋아요가 취소되었습니다.");
        serviceResult.put("likeCount", 4);
        given(postReactionService.togglePostLike(1L, "user@haru.com")).willReturn(serviceResult);

        mockMvc.perform(post("/api/core/boards/{postId}/like", 1)
                        .header("Authorization", "Bearer ok"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.liked").value(false))
                .andExpect(jsonPath("$.data.likeCount").value(4));
    }

    @Test
    @DisplayName("toggleLike: 예외 발생 시 500(본문 문자열은 단언하지 않음 — addReaction/deleteReaction과 동일 패턴)")
    void toggleLike_exception_returns500() throws Exception {
        given(tokenUtils.getEmailFromAuthHeader(anyString())).willReturn("user@haru.com");
        given(postReactionService.togglePostLike(anyLong(), anyString()))
                .willThrow(new RuntimeException("DB 오류"));

        mockMvc.perform(post("/api/core/boards/{postId}/like", 1)
                        .header("Authorization", "Bearer ok"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("500"));
    }
}
