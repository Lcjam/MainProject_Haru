package com.example.demo.service.Market;

import com.example.demo.Application;
import com.example.demo.mapper.Market.UserLocationMapper;
import com.example.demo.model.Market.UserLocation;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class UserLocationServiceTest {
    private final UserLocationMapper mapper = mock(UserLocationMapper.class);
    private final UserLocationService service = new UserLocationService(mapper);

    @Test
    void updateAcceptsCoordinateBoundaries() {
        UserLocation location = location(-90.0, 180.0);

        service.updateUserLocation(location);

        verify(mapper).insertOrUpdateUserLocation(location);
    }

    @Test
    void updateRejectsMissingNonFiniteAndOutOfRangeCoordinates() {
        for (UserLocation location : List.of(
                location(null, 0.0), location(0.0, null),
                location(Double.NaN, 0.0), location(0.0, Double.POSITIVE_INFINITY),
                location(-90.000001, 0.0), location(0.0, 180.000001))) {
            assertThrows(IllegalArgumentException.class, () -> service.updateUserLocation(location));
        }
    }

    @Test
    void cleanupIsScheduledAndSchedulingIsEnabled() throws Exception {
        Scheduled scheduled = UserLocationService.class.getMethod("deleteOldUserLocations")
                .getAnnotation(Scheduled.class);
        assertNotNull(scheduled);
        assertEquals("0 0 3 * * ?", scheduled.cron());
        assertNotNull(Application.class.getAnnotation(EnableScheduling.class));

        service.deleteOldUserLocations();
        verify(mapper).deleteOldUserLocations();
        verify(mapper).deleteLegacyOldUserLocations();
    }

    private UserLocation location(Double latitude, Double longitude) {
        return UserLocation.builder().email("user@haru.com").latitude(latitude).longitude(longitude).build();
    }
}
