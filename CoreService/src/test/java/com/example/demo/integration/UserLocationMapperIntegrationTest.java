package com.example.demo.integration;

import com.example.demo.integration.support.MySqlTestDatabase;
import com.example.demo.mapper.Market.UserLocationMapper;
import com.example.demo.model.Market.UserLocation;
import com.example.demo.service.Market.UserLocationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@Tag("mysql")
class UserLocationMapperIntegrationTest {
    private static final String USER = "location@d08.example.test";
    private static final String OLD = "old-location@d08.example.test";
    private MySqlTestDatabase database;
    private UserLocationMapper mapper;

    @BeforeEach
    void setUp() throws Exception {
        database = MySqlTestDatabase.create();
        database.jdbc().update("""
                INSERT INTO Users (email,password_hash,name,nickname,login_method,account_status)
                VALUES (?, 'fixture', 'D08 User', 'd08-user', 'EMAIL', 'Active'),
                       (?, 'fixture', 'D08 Old', 'd08-old', 'EMAIL', 'Active')
                """, USER, OLD);
        mapper = database.sqlSession().getMapper(UserLocationMapper.class);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (database != null) database.close();
    }

    @Test
    void legacyFallbackIsDeterministicThenUpsertKeepsOneLatestRow() {
        database.jdbc().update("""
                INSERT INTO UserLocation (email,location_name,latitude,longitude,recorded_at)
                VALUES (?, 'first', 37.1, 127.1, NOW()), (?, 'second', 37.2, 127.2, NOW())
                """, USER, USER);
        assertEquals("second", mapper.getUserLatestLocation(USER).getLocationName());

        mapper.insertOrUpdateUserLocation(location(USER, "new", 38.0, 128.0));
        mapper.insertOrUpdateUserLocation(location(USER, "updated", 39.0, 129.0));

        UserLocation latest = mapper.getUserLatestLocation(USER);
        assertEquals("updated", latest.getLocationName());
        assertEquals(39.0, latest.getLatitude());
        assertEquals(1, database.jdbc().queryForObject(
                "SELECT COUNT(*) FROM UserLocationLatest WHERE email=?", Integer.class, USER));
        assertEquals(2, database.jdbc().queryForObject(
                "SELECT COUNT(*) FROM UserLocation WHERE email=?", Integer.class, USER));
    }

    @Test
    void cleanupDeletesExpiredLatestAndExpiredLegacyIsNotReturned() {
        mapper.insertOrUpdateUserLocation(location(USER, "expired", 37.0, 127.0));
        database.jdbc().update("UPDATE UserLocationLatest SET recorded_at=NOW()-INTERVAL 15 DAY WHERE email=?", USER);
        database.jdbc().update("""
                INSERT INTO UserLocation (email,location_name,latitude,longitude,recorded_at)
                VALUES (?, 'legacy-expired', 37.0, 127.0, NOW()-INTERVAL 15 DAY)
                """, OLD);

        new UserLocationService(mapper).deleteOldUserLocations();

        assertEquals(0, database.jdbc().queryForObject(
                "SELECT COUNT(*) FROM UserLocationLatest WHERE email=?", Integer.class, USER));
        assertEquals(0, database.jdbc().queryForObject(
                "SELECT COUNT(*) FROM UserLocation WHERE email=?", Integer.class, OLD));
        assertNull(mapper.getUserLatestLocation(USER));
        assertNull(mapper.getUserLatestLocation(OLD));
    }

    private UserLocation location(String email, String name, double latitude, double longitude) {
        return UserLocation.builder().email(email).locationName(name)
                .latitude(latitude).longitude(longitude).build();
    }
}
