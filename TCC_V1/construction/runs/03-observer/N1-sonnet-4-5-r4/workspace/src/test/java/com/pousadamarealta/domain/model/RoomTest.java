package com.pousadamarealta.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class RoomTest {

    @Test
    void testStandardRoom() {
        Room room = new Room("101", RoomType.STANDARD);

        assertEquals("101", room.getNumber());
        assertEquals(RoomType.STANDARD, room.getType());
        assertEquals(2, room.getCapacity());
        assertEquals(new BigDecimal("249.90"), room.getNightlyRate());
    }

    @Test
    void testDeluxeRoom() {
        Room room = new Room("201", RoomType.DELUXE);

        assertEquals("201", room.getNumber());
        assertEquals(RoomType.DELUXE, room.getType());
        assertEquals(3, room.getCapacity());
        assertEquals(new BigDecimal("389.90"), room.getNightlyRate());
    }

    @Test
    void testSuiteRoom() {
        Room room = new Room("301", RoomType.SUITE);

        assertEquals("301", room.getNumber());
        assertEquals(RoomType.SUITE, room.getType());
        assertEquals(4, room.getCapacity());
        assertEquals(new BigDecimal("579.90"), room.getNightlyRate());
    }
}
