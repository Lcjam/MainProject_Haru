package com.example.demo.service;

import com.example.demo.mapper.LocationMapper;
import com.example.demo.mapper.UserMapper;
import com.example.demo.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * 단위 1a-3 — LocationController.handleLocationUpdate({@code @MessageMapping}) 가 직접 쓰던
 * UserMapper.findByEmail 을 LocationService 로 옮긴 뒤의 통과 메서드 단위 테스트. MockMvc 가
 * {@code @MessageMapping} 핸들러를 태우지 못해 컨트롤러 계약 테스트에서 검증할 수 없으므로
 * 여기서 직접 검증한다.
 */
@DisplayName("LocationService 신규 통과 메서드")
class LocationServiceTest {

    private final LocationMapper locationMapper = mock(LocationMapper.class);
    private final UserMapper userMapper = mock(UserMapper.class);

    private final LocationService locationService = new LocationService(locationMapper, userMapper);

    @Test
    @DisplayName("findUserByEmail: UserMapper.findByEmail 를 그대로 위임한다")
    void findUserByEmail_delegatesToMapper() {
        User user = User.builder().email("sender@haru.com").nickname("sender-nick").build();
        given(userMapper.findByEmail("sender@haru.com")).willReturn(user);

        User result = locationService.findUserByEmail("sender@haru.com");

        assertEquals("sender-nick", result.getNickname());
    }

    @Test
    @DisplayName("findUserByEmail: 사용자가 없으면 null 을 그대로 반환한다")
    void findUserByEmail_returnsNullWhenNotFound() {
        given(userMapper.findByEmail("missing@haru.com")).willReturn(null);

        assertNull(locationService.findUserByEmail("missing@haru.com"));
    }
}
