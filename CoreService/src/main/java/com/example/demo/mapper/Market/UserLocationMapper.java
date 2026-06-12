package com.example.demo.mapper.Market;

import com.example.demo.model.Market.UserLocation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserLocationMapper {
    /** 사용자의 위치를 저장하거나 업데이트 **/
    void insertOrUpdateUserLocation(UserLocation userLocation);

    /** 사용자의 최신 위치 정보를 가져옴 **/
    UserLocation getUserLatestLocation(@Param("email") String email);

    /** 14일 이상 지난 위치 데이터를 삭제 **/
    void deleteOldUserLocations();
}