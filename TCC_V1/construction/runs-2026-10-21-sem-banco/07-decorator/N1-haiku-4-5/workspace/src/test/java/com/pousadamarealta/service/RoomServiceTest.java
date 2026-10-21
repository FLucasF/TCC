package com.pousadamarealta.service;

import com.pousadamarealta.domain.Room;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

public class RoomServiceTest {
    @Test
    public void testInitializeRooms() {
        RoomService service = new RoomService();
        List<Room> rooms = service.getAllRooms();

        assertEquals(5, rooms.size());
    }

    @Test
    public void testGetRoomByNumber() {
        RoomService service = new RoomService();

        Optional<Room> room101 = service.getRoomByNumber(101);
        assertTrue(room101.isPresent());
        assertEquals("STANDARD", room101.get().getType());
        assertEquals(2, room101.get().getCapacity());

        Optional<Room> room301 = service.getRoomByNumber(301);
        assertTrue(room301.isPresent());
        assertEquals("SUITE", room301.get().getType());
        assertEquals(4, room301.get().getCapacity());

        Optional<Room> room999 = service.getRoomByNumber(999);
        assertFalse(room999.isPresent());
    }
}
