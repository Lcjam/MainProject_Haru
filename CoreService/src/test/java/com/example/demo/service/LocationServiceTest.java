package com.example.demo.service;

import com.example.demo.exception.NotFoundException;
import com.example.demo.mapper.ChatRoomMapper;
import com.example.demo.mapper.LocationMapper;
import com.example.demo.mapper.UserMapper;
import com.example.demo.model.Location;
import com.example.demo.model.User;
import com.example.demo.model.chat.ChatRoom;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

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
    private final ChatRoomMapper chatRoomMapper = mock(ChatRoomMapper.class);

    private final LocationService locationService = new LocationService(locationMapper, userMapper, chatRoomMapper);

    @Test
    @DisplayName("saveLocation: 방 멤버의 경계 좌표를 서버 시각으로 저장한다")
    void saveLocation_acceptsBoundaryCoordinatesForMember() {
        Location location = location(7, "member@haru.com", -90.0, 180.0);
        given(chatRoomMapper.findChatRoomById(7, "member@haru.com")).willReturn(new ChatRoom());
        given(userMapper.findByEmail("member@haru.com")).willReturn(active("member@haru.com"));

        locationService.saveLocation(location);

        assertNotNull(location.getTimestamp());
        verify(locationMapper).saveLocation(location);
    }

    @Test
    @DisplayName("saveLocation: null·NaN·Infinity·범위 밖 좌표를 저장하지 않는다")
    void saveLocation_rejectsInvalidCoordinates() {
        Location[] invalid = {
                null,
                location(7, "member@haru.com", null, 127.0),
                location(7, "member@haru.com", 37.0, null),
                location(7, "member@haru.com", Double.NaN, 127.0),
                location(7, "member@haru.com", 37.0, Double.POSITIVE_INFINITY),
                location(7, "member@haru.com", 90.000001, 127.0),
                location(7, "member@haru.com", 37.0, -180.000001)
        };

        for (Location location : invalid) {
            assertThrows(IllegalArgumentException.class, () -> locationService.saveLocation(location));
        }
        verifyNoInteractions(chatRoomMapper, locationMapper);
    }

    @Test
    @DisplayName("saveLocation: 방 멤버가 아니면 저장하지 않는다")
    void saveLocation_rejectsNonMember() {
        Location location = location(7, "outsider@haru.com", 37.0, 127.0);

        assertThrows(NotFoundException.class, () -> locationService.saveLocation(location));

        verify(locationMapper, never()).saveLocation(location);
    }

    @Test
    @DisplayName("getRecentLocations: 요청자가 방 멤버일 때만 조회한다")
    void getRecentLocations_requiresRequesterMembership() {
        given(chatRoomMapper.findChatRoomById(7, "member@haru.com")).willReturn(new ChatRoom());
        given(userMapper.findByEmail("member@haru.com")).willReturn(active("member@haru.com"));

        locationService.getRecentLocations(7, "member@haru.com");

        verify(locationMapper).getRecentLocations(7);
        assertThrows(NotFoundException.class,
                () -> locationService.getRecentLocations(7, "outsider@haru.com"));
    }

    @Test
    @DisplayName("getLastLocation: 요청자와 대상자가 모두 같은 방 멤버여야 한다")
    void getLastLocation_requiresRequesterAndTargetMembership() {
        given(chatRoomMapper.findChatRoomById(7, "requester@haru.com")).willReturn(new ChatRoom());
        given(chatRoomMapper.findChatRoomById(7, "target@haru.com")).willReturn(new ChatRoom());
        given(userMapper.findByEmail("requester@haru.com")).willReturn(active("requester@haru.com"));

        locationService.getLastLocation(7, "target@haru.com", "requester@haru.com");

        verify(locationMapper).getLastLocation(7, "target@haru.com");
        assertThrows(NotFoundException.class,
                () -> locationService.getLastLocation(7, "outsider@haru.com", "requester@haru.com"));
    }

    @Test
    @DisplayName("탈퇴한 방 멤버의 위치 저장·조회는 거부한다")
    void withdrawnMemberCannotSaveOrRead() {
        String email = "withdrawn@haru.com";
        given(chatRoomMapper.findChatRoomById(7, email)).willReturn(new ChatRoom());
        given(userMapper.findByEmail(email)).willReturn(
                User.builder().email(email).accountStatus("Withdrawal").build());

        assertThrows(NotFoundException.class,
                () -> locationService.saveLocation(location(7, email, 37.0, 127.0)));
        assertThrows(NotFoundException.class,
                () -> locationService.getRecentLocations(7, email));
        assertThrows(NotFoundException.class,
                () -> locationService.getLastLocation(7, email, email));
        verifyNoInteractions(locationMapper);
    }

    @Test
    @DisplayName("deleteOldLocations: 24시간 정리 SQL을 위임한다")
    void deleteOldLocations_delegatesToMapper() {
        locationService.deleteOldLocations();

        verify(locationMapper).deleteOldLocations();
    }

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

    private static Location location(Integer chatroomId, String email, Double latitude, Double longitude) {
        Location location = new Location();
        location.setChatroomId(chatroomId);
        location.setEmail(email);
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        location.setTimestamp(LocalDateTime.MIN);
        return location;
    }

    private static User active(String email) {
        return User.builder().email(email).accountStatus("Active").build();
    }
}
