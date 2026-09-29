package com.example.demo.integration;

import com.example.demo.integration.support.MySqlTestDatabase;
import com.example.demo.mapper.LocationMapper;
import com.example.demo.model.Location;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("mysql")
class LocationMapperIntegrationTest {

    private static final String HOST = "host@r03.example.test";
    private static final String MEMBER = "member@r03.example.test";
    private static final String OUTSIDER = "outsider@r03.example.test";
    private MySqlTestDatabase database;
    private LocationMapper locations;

    @BeforeEach
    void setUp() throws Exception {
        database = MySqlTestDatabase.create();
        database.seedFixture();
        locations = database.sqlSession().getMapper(LocationMapper.class);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (database != null) database.close();
    }

    @Test
    void recentAndLast_returnDeterministicLatestLocationForEachCurrentMember() {
        LocalDateTime now = database.jdbc().queryForObject("SELECT NOW()", LocalDateTime.class);
        locations.saveLocation(location(HOST, 35.0, now.minusMinutes(6)));
        locations.saveLocation(location(HOST, 36.0, now.minusMinutes(1)));
        locations.saveLocation(location(MEMBER, 37.0, now));
        locations.saveLocation(location(MEMBER, 38.0, now));
        locations.saveLocation(location(OUTSIDER, 39.0, now));

        List<Location> recent = locations.getRecentLocations(300);

        assertEquals(List.of(MEMBER, HOST), recent.stream().map(Location::getEmail).toList());
        assertEquals(38.0, recent.get(0).getLatitude());
        assertEquals(38.0, locations.getLastLocation(300, MEMBER).getLatitude());
    }

    @Test
    void deleteOldLocations_removesOnlyRowsOlderThan24Hours() {
        LocalDateTime now = database.jdbc().queryForObject("SELECT NOW()", LocalDateTime.class);
        locations.saveLocation(location(HOST, 35.0, now.minusHours(25)));
        locations.saveLocation(location(MEMBER, 36.0, now.minusHours(23)));

        locations.deleteOldLocations();

        assertEquals(1, database.jdbc().queryForObject("SELECT COUNT(*) FROM locations", Integer.class));
        assertEquals(MEMBER, database.jdbc().queryForObject("SELECT email FROM locations", String.class));
    }

    private static Location location(String email, double latitude, LocalDateTime timestamp) {
        Location location = new Location();
        location.setChatroomId(300);
        location.setEmail(email);
        location.setLatitude(latitude);
        location.setLongitude(127.0);
        location.setTimestamp(timestamp);
        return location;
    }
}
