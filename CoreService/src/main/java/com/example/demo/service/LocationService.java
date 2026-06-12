package com.example.demo.service;

import com.example.demo.mapper.LocationMapper;
import com.example.demo.model.Location;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class LocationService {

    private final LocationMapper locationMapper;

    /**
     * 위치 정보 저장
     */
    @Transactional
    public void saveLocation(Location location) {
        try {
            locationMapper.saveLocation(location);
            log.info("위치 정보 저장 완료: chatroomId={}, email={}", 
                    location.getChatroomId(), location.getEmail());
        } catch (Exception e) {
            log.error("위치 정보 저장 중 오류 발생: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 채팅방의 최근 위치 정보 조회
     */
    public List<Location> getRecentLocations(Integer chatroomId) {
        try {
            return locationMapper.getRecentLocations(chatroomId);
        } catch (Exception e) {
            log.error("최근 위치 정보 조회 중 오류 발생: chatroomId={}, error={}", 
                    chatroomId, e.getMessage());
            throw e;
        }
    }

    /**
     * 특정 사용자의 마지막 위치 조회
     */
    public Location getLastLocation(Integer chatroomId, String email) {
        try {
            return locationMapper.getLastLocation(chatroomId, email);
        } catch (Exception e) {
            log.error("마지막 위치 정보 조회 중 오류 발생: chatroomId={}, email={}, error={}", 
                    chatroomId, email, e.getMessage());
            throw e;
        }
    }
} 