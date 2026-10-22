package com.pousadamarealta;

import com.pousadamarealta.db.Database;
import com.pousadamarealta.model.Room;
import com.pousadamarealta.repository.RoomRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SimpleTest {
    @Test
    void testDatabaseConnection() {
        String dbUrl = "jdbc:h2:mem:simpletest;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1";
        String dbUser = "sa";
        String dbPassword = "";

        Database database = new Database(dbUrl, dbUser, dbPassword);
        database.initialize();

        RoomRepository roomRepository = new RoomRepository(database);
        List<Room> rooms = roomRepository.findAll();

        assertEquals(5, rooms.size());
        assertEquals("101", rooms.get(0).getNumber());
    }
}
