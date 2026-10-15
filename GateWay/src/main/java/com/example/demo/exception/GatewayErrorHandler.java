package com.example.demo.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.timeout.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.Map;

@Component
@Order(-2)
public class GatewayErrorHandler implements ErrorWebExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GatewayErrorHandler.class);
    private final ObjectMapper mapper;

    public GatewayErrorHandler(ObjectMapper mapper) { this.mapper = mapper; }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable error) {
        if (exchange.getResponse().isCommitted()) return Mono.error(error);
        ApiErrorCode code = classify(error);
        String path = exchange.getRequest().getPath().value();
        if (code == ApiErrorCode.INTERNAL_ERROR) {
            log.error("Gateway request failed: path={}", path, error);
        } else if (code == ApiErrorCode.UPSTREAM_UNAVAILABLE || code == ApiErrorCode.UPSTREAM_TIMEOUT) {
            log.warn("Gateway upstream request failed: path={}, exception={}", path, error.getClass().getName());
        } else {
            log.debug("Gateway client error: path={}, code={}", path, code);
        }
        return writeError(exchange, mapper, code);
    }

    static ApiErrorCode classify(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof TimeoutException || cause instanceof java.util.concurrent.TimeoutException
                    || cause instanceof SocketTimeoutException || cause instanceof io.netty.channel.ConnectTimeoutException) {
                return ApiErrorCode.UPSTREAM_TIMEOUT;
            }
            if (cause instanceof ConnectException || cause instanceof java.net.UnknownHostException
                    || cause instanceof java.nio.channels.UnresolvedAddressException
                    || cause instanceof reactor.netty.http.client.PrematureCloseException) return ApiErrorCode.UPSTREAM_UNAVAILABLE;
        }
        if (error instanceof ResponseStatusException status) return ApiErrorCode.fromStatus(status.getStatusCode().value());
        return ApiErrorCode.INTERNAL_ERROR;
    }

    public static Mono<Void> writeError(ServerWebExchange exchange, ObjectMapper mapper, ApiErrorCode code) {
        exchange.getResponse().setStatusCode(HttpStatusCode.valueOf(code.httpStatus()));
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        try {
            byte[] body = mapper.writeValueAsBytes(Map.of("status", "error", "code", code.name(), "message", code.message()));
            return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(body)));
        } catch (java.io.IOException exception) {
            return Mono.error(exception);
        }
    }
}
