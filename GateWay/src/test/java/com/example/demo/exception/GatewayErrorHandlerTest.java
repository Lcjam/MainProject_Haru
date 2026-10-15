package com.example.demo.exception;

import org.junit.jupiter.api.Test;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.LoggerFactory;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.net.ConnectException;
import java.util.concurrent.TimeoutException;
import static org.junit.jupiter.api.Assertions.*;

class GatewayErrorHandlerTest {
    @Test
    void errorsLogAtDiagnosticLevelsWithoutExposingDetailsInResponses() {
        Logger logger = (Logger) LoggerFactory.getLogger(GatewayErrorHandler.class);
        Level previous = logger.getLevel();
        var appender = new ListAppender<ILoggingEvent>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.DEBUG);
        try {
            var handler = new GatewayErrorHandler(new ObjectMapper());
            Throwable[] errors = {new IllegalStateException("SQL private"), new ConnectException("private host"),
                    new TimeoutException("private timeout"), new ResponseStatusException(HttpStatus.NOT_FOUND),
                    new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED)};
            Level[] levels = {Level.ERROR, Level.WARN, Level.WARN, Level.DEBUG, Level.DEBUG};
            for (int i = 0; i < errors.length; i++) {
                var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/core/probe"));
                handler.handle(exchange, errors[i]).block();
                ILoggingEvent logged = appender.list.get(i);
                assertEquals(levels[i], logged.getLevel());
                assertTrue(logged.getFormattedMessage().contains("/api/core/probe"));
                if (i == 0) assertNotNull(logged.getThrowableProxy());
                else {
                    assertNull(logged.getThrowableProxy());
                    if (i <= 2) assertTrue(logged.getFormattedMessage().contains(errors[i].getClass().getName()));
                }
                String body = exchange.getResponse().getBodyAsString().block();
                assertFalse(body.contains("private"));
                assertFalse(body.contains("Exception"));
            }
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(previous);
            appender.stop();
        }
    }

    @Test
    void statusExceptionsAndWrappedNetworkFailuresAreClassifiedWithoutTheirMessages() {
        assertEquals(ApiErrorCode.METHOD_NOT_ALLOWED, GatewayErrorHandler.classify(new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED, "SQL Exception")));
        assertEquals(ApiErrorCode.UNSUPPORTED_MEDIA_TYPE, GatewayErrorHandler.classify(new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE)));
        assertEquals(ApiErrorCode.UPSTREAM_UNAVAILABLE, GatewayErrorHandler.classify(new RuntimeException(new ConnectException("SQL private host"))));
        assertEquals(ApiErrorCode.UPSTREAM_TIMEOUT, GatewayErrorHandler.classify(new RuntimeException(new TimeoutException("private timeout"))));
        assertEquals(ApiErrorCode.UPSTREAM_UNAVAILABLE, GatewayErrorHandler.classify(new ResponseStatusException(HttpStatus.BAD_GATEWAY)));
        assertEquals(ApiErrorCode.UPSTREAM_UNAVAILABLE, GatewayErrorHandler.classify(new java.net.UnknownHostException("private host")));
        assertEquals(ApiErrorCode.INTERNAL_ERROR, GatewayErrorHandler.classify(new IllegalStateException("SQL private")));
    }
}
