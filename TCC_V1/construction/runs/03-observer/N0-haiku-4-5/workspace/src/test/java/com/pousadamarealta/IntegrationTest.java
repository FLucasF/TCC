package com.pousadamarealta;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class IntegrationTest {
    @Test
    public void testDatabaseInitialization() {
        try {
            Database.initialize();
            var rooms = ReservationService.getRooms();
            assertEquals(5, rooms.size());
            assertEquals(101, rooms.get(0).number);
            assertEquals(301, rooms.get(4).number);
        } catch (Exception e) {
            fail("Database initialization failed: " + e.getMessage());
        }
    }
}
