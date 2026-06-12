package com.example.demo.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import com.example.demo.filter.JwtAuthenticationFilter;

import org.springframework.http.HttpHeaders;

@Configuration
public class RouteConfig {
    @Autowired
    private JwtAuthenticationFilter jwtFilter;

    // 로컬 개발은 기본 dev(localhost), 도커 배포 시 GATEWAY_PROFILE=prod 로 덮어쓰기
    @Value("${GATEWAY_PROFILE:dev}")
    private String activeProfile;

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        System.out.println("=== RouteConfig 초기화 ===");
        System.out.println("현재 활성화된 프로필: " + activeProfile);

        final String coreUri = "prod".equals(activeProfile)
                ? "http://core-container:8081" // 도커네트워크브릿지 사용하지 않고 외부 도메인 사용 시 sunbee.world 사용하는 거처럼 외부에 독립적인 서버 설정 가능
                : "http://localhost:8081"; // 이 url로 전달 됩니다다

        final String assistUri = "prod".equals(activeProfile)
                ? "http://assist-container:8082"
                : "http://localhost:8082";

        final String fastapiUri = "prod".equals(activeProfile)
                ? "http://fastapi-container:8001"
                : "http://localhost:8001";

        final String webSocketUri = "prod".equals(activeProfile)
                ? "ws://core-container:8081" // 도커 브릿지 네트워크로 사용하기 때문에 wss 사용 해도 되지만 ws도 가능
                : "ws://localhost:8081";

        System.out.println("Core URI: " + coreUri);
        System.out.println("Assist URI: " + assistUri);
        System.out.println("FastAPI URI: " + fastapiUri);
        System.out.println("WebSocket URI: " + webSocketUri);
        
        // 여기서 엔드포인트 이름에 따라 요청이 분배됩니다다
        return builder.routes()
                .route("coreService", r -> r
                        .path("/api/core/**")
                        .filters(f -> f.filter(jwtFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri(coreUri))
                // 여기 url로 가게 되는데데
                .route("assistService", r -> r
                        .path("/api/assist/**")
                        .filters(f -> f.filter(jwtFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri(assistUri))
                .route("fastapiService", r -> r
                        .path("/api/fastapi/**")
                        .filters(f -> f.filter(jwtFilter.apply(new JwtAuthenticationFilter.Config())))
                        .uri(fastapiUri))
                // 웹소켓 라우팅 개선
                .route("coreSockJsWebSocket", r -> r
                        .path("/ws/**", "/ws", "/topic/**")
                        .filters(f -> f
                                // 웹소켓 헤더 보존 및 추가
                                .preserveHostHeader() // 제거해도 될듯?
                                .removeRequestHeader(HttpHeaders.HOST)
                                .addRequestHeader("Connection", "Upgrade")
                                .addRequestHeader("Upgrade", "websocket")                            
                                .addRequestHeader("Sec-WebSocket-Version", "13")
                                // 인증 필터 제외 (JWT 인증은 STOMP 단에서 처리)
                        )
                        .uri(webSocketUri))
                .build();
                
    }
}
