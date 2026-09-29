package com.example.demo.service;

import com.example.demo.exception.NotFoundException;
import com.example.demo.mapper.ChatRoomMapper;
import com.example.demo.mapper.LocationMapper;
import com.example.demo.mapper.UserMapper;
import com.example.demo.model.Location;
import com.example.demo.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class LocationService {

    private final LocationMapper locationMapper;
    private final UserMapper userMapper;
    private final ChatRoomMapper chatRoomMapper;

    /**
     * 위치 정보 저장
     */
    @Transactional
    public void saveLocation(Location location) {
        validateCoordinates(location);
        requireActiveMember(location.getChatroomId(), location.getEmail());
        location.setTimestamp(LocalDateTime.now());
        locationMapper.saveLocation(location);
        log.info("위치 정보 저장 완료: chatroomId={}, email={}",
                location.getChatroomId(), location.getEmail());
    }

    /**
     * 채팅방의 최근 위치 정보 조회
     */
    public List<Location> getRecentLocations(Integer chatroomId, String requestEmail) {
        requireActiveMember(chatroomId, requestEmail);
        return locationMapper.getRecentLocations(chatroomId);
    }

    /**
     * 특정 사용자의 마지막 위치 조회
     */
    public Location getLastLocation(Integer chatroomId, String email, String requestEmail) {
        requireActiveMember(chatroomId, requestEmail);
        requireMember(chatroomId, email);
        return locationMapper.getLastLocation(chatroomId, email);
    }

    /**
     * 이메일로 사용자 조회 (LocationController.handleLocationUpdate 의 알림 메시지용 닉네임 조회)
     */
    public User findUserByEmail(String email) {
        return userMapper.findByEmail(email);
    }

    @Scheduled(cron = "0 0 3 * * ?")
    public void deleteOldLocations() {
        locationMapper.deleteOldLocations();
    }

    private void requireMember(Integer chatroomId, String email) {
        if (chatroomId == null || email == null || chatRoomMapper.findChatRoomById(chatroomId, email) == null) {
            throw new NotFoundException("채팅방을 찾을 수 없습니다.");
        }
    }

    private void requireActiveMember(Integer chatroomId, String email) {
        requireMember(chatroomId, email);
        User user = userMapper.findByEmail(email);
        if (user == null || !"Active".equals(user.getAccountStatus())) {
            throw new NotFoundException("채팅방을 찾을 수 없습니다.");
        }
    }

    private void validateCoordinates(Location location) {
        if (location == null
                || location.getLatitude() == null || !Double.isFinite(location.getLatitude())
                || location.getLatitude() < -90 || location.getLatitude() > 90
                || location.getLongitude() == null || !Double.isFinite(location.getLongitude())
                || location.getLongitude() < -180 || location.getLongitude() > 180) {
            throw new IllegalArgumentException("유효한 위도와 경도가 필요합니다.");
        }
    }
}
