package com.example.demo.exception;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class ApiErrorCodeFixtureTest {
    @Test
    void allCodesAndLegacyMappingsMatchSharedFixture() throws Exception {
        JsonNode fixture = new ObjectMapper().readTree(Path.of("../contracts/api-error-fixtures.json").toFile());
        assertEquals(fixture.get("codes").size(), ApiErrorCode.values().length);
        for (ApiErrorCode code : ApiErrorCode.values()) {
            JsonNode expected = fixture.get("codes").get(code.name());
            assertNotNull(expected, code.name());
            assertEquals(expected.get("http").asInt(), code.httpStatus(), code.name());
            assertEquals(expected.get("message").asText(), code.message(), code.name());
            assertEquals(expected.get("fixedMessage").asBoolean(), code.fixedMessage(), code.name());
        }
        fixture.get("legacyNumericCodes").fields().forEachRemaining(entry ->
                assertEquals(entry.getValue().asText(), ApiErrorCode.resolve(entry.getKey(), 500).name()));
    }
}
