package com.example.demo.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;

import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

// compose(GATEWAY_PROFILE=prod)에서는 compose 네트워크 별칭으로만 라우팅하고 외부 도메인을 쓰지 않는다.
@SpringBootTest(properties = {"GATEWAY_PROFILE=prod", "jwt.secret=route-config-profile-secret-long-enough"})
class RouteConfigProfileTest {

    @Autowired
    RouteLocator routeLocator;

    @Test
    void prodProfileRoutesToComposeAliases() {
        Map<String, String> uris = routeLocator.getRoutes().collectList().block().stream()
                .collect(Collectors.toMap(Route::getId, route -> route.getUri().toString()));

        assertThat(uris).containsEntry("coreService", "http://core-container:8081")
                .containsEntry("assistService", "http://assist-container:8082")
                .containsEntry("fastapiService", "http://fastapi-container:8001")
                .containsEntry("coreSockJsWebSocket", "ws://core-container:8081");
    }
}
