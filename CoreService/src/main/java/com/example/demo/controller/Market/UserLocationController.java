package com.example.demo.controller.Market;

import com.example.demo.dto.Market.LocationRequest;
import com.example.demo.mapper.UserMapper;
import com.example.demo.model.User;
import com.example.demo.model.Market.UserLocation;
import com.example.demo.service.Market.UserLocationService;
import com.example.demo.util.BaseResponse;
import com.example.demo.util.TokenUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/core/market/users")
public class UserLocationController {
    private final UserLocationService userLocationService;
    private final TokenUtils tokenUtils;
    private final UserMapper userMapper;

    public UserLocationController(UserLocationService userLocationService, TokenUtils tokenUtils, UserMapper userMapper) {
        this.userLocationService = userLocationService;
        this.tokenUtils = tokenUtils;
        this.userMapper = userMapper;
    }

    /**
     * 사용자의 위치 정보를 업데이트 (등록 또는 수정)
     */
    @PostMapping("/location")
    public ResponseEntity<BaseResponse<String>> updateUserLocation(
            @RequestBody(required = false) LocationRequest request,
            @RequestHeader(value = "Authorization", required = false) String token) {

        // 토큰 검증
        if (token == null || !token.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(BaseResponse.error("토큰이 누락되었습니다. 인증이 필요합니다."));
        }
        if (request == null) {
            return ResponseEntity.badRequest().body(BaseResponse.error("위치 정보가 누락되었습니다."));
        }

        String email = activeEmail(token);
        if (email == null) {
            return ResponseEntity.status(401).body(BaseResponse.error("유효하지 않은 토큰입니다."));
        }

        // 위치 정보 객체 생성 및 저장
        UserLocation location = new UserLocation();
        location.setEmail(email);
        location.setLatitude(request.getLatitude());
        location.setLongitude(request.getLongitude());
        location.setLocationName(request.getLocationName());

        try {
            userLocationService.updateUserLocation(location);
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(BaseResponse.error(exception.getMessage()));
        }

        return ResponseEntity.ok(new BaseResponse<>("사용자 위치 업데이트 완료"));
    }

    /**
     * 사용자의 최신 위치 정보를 조회
     */
    @GetMapping("/location/latest")
    public ResponseEntity<BaseResponse<UserLocation>> getUserLatestLocation(
            @RequestHeader(value = "Authorization", required = false) String token) {

        // 토큰 검증
        if (token == null || !token.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(BaseResponse.error("토큰이 누락되었습니다. 인증이 필요합니다."));
        }

        String email = activeEmail(token);
        if (email == null) {
            return ResponseEntity.status(401).body(BaseResponse.error("유효하지 않은 토큰입니다."));
        }

        // 최신 위치 정보 가져오기
        UserLocation latestLocation = userLocationService.getUserLatestLocation(email);

        // 사용자의 위치 정보가 없는 경우 예외 처리
        if (latestLocation == null) {
            return ResponseEntity.ok(new BaseResponse<>(null, "사용자의 위치 정보가 존재하지 않습니다."));
        }

        return ResponseEntity.ok(new BaseResponse<>(latestLocation, "사용자의 최신 위치 조회 완료"));
    }

    private String activeEmail(String token) {
        String email = tokenUtils.getEmailFromAuthHeader(token);
        User user = email == null ? null : userMapper.findByEmail(email);
        return user != null && "Active".equals(user.getAccountStatus()) ? email : null;
    }
}
