package com.example.demo.service.Market;

import com.example.demo.mapper.Market.UserLocationMapper;
import com.example.demo.model.Market.UserLocation;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 사용자 위치 관련 로직을 처리하는 서비스 클래스
 */
@Service
public class UserLocationService {
    private final UserLocationMapper userLocationMapper;

    public UserLocationService(UserLocationMapper userLocationMapper) {
        this.userLocationMapper = userLocationMapper;
    }

    /**
     * 사용자의 위치 정보를 저장하거나 업데이트
     */
    public void updateUserLocation(UserLocation location) {
        userLocationMapper.insertOrUpdateUserLocation(location);
    }

    /**
     * 사용자의 최신 위치 정보를 조회
     */
    public UserLocation getUserLatestLocation(String email) {
        return userLocationMapper.getUserLatestLocation(email);

    }

    /**
     * 14일 이상 지난 위치 데이터를 자동 삭제하는 스케줄러 (매일 새벽 3시 실행)
     */
    @Scheduled(cron = "0 0 3 * * ?") // 매일 새벽 3시 실행
    public void deleteOldUserLocations() {
        userLocationMapper.deleteOldUserLocations();
        System.out.println("14일 이상 된 사용자 위치 데이터 삭제 완료!");
    }
}
