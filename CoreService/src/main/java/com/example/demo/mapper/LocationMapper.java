package com.example.demo.mapper;

import com.example.demo.model.Location;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LocationMapper {
    
    /**
     * 위치 정보 저장
     */
    void saveLocation(Location location);

    /**
     * 채팅방의 최근 위치 정보 조회 (최근 24시간)
     */
    List<Location> getRecentLocations(@Param("chatroomId") Integer chatroomId);

    /**
     * 특정 사용자의 마지막 위치 조회
     */
    Location getLastLocation(
        @Param("chatroomId") Integer chatroomId,
        @Param("email") String email
    );

    /**
     * 오래된 위치 데이터 삭제
     */
    void deleteOldLocations();
} 