package com.pousadamarealta.api;

import com.google.gson.Gson;
import com.pousadamarealta.api.request.CreateReservationRequest;
import com.pousadamarealta.pricing.*;
import com.pousadamarealta.domain.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class RoutesTest {
    @Test
    public void testJsonParsing() {
        Gson gson = new Gson();
        String json = "{\"roomNumber\":101,\"guestName\":\"John\",\"guestEmail\":\"j@test.com\",\"guestCpf\":\"12345678901\",\"checkIn\":\"2027-03-10\",\"checkOut\":\"2027-03-12\",\"guests\":2,\"ratePlan\":\"STANDARD\"}";

        CreateReservationRequest req = gson.fromJson(json, CreateReservationRequest.class);
        assertEquals(101, req.getRoomNumber());
        assertEquals("John", req.getGuestName());
    }

    @Test
    public void testMoneyFormatting() {
        BigDecimal value = new BigDecimal("249.90");
        value = value.multiply(BigDecimal.valueOf(1.20));
        value = MoneyRounder.round(value);

        assertEquals(0, new BigDecimal("299.88").compareTo(value));
    }

    @Test
    public void testSeasonDetection() {
        assertTrue(Season.isHighSeason(LocalDate.of(2027, 7, 15)));
        assertTrue(Season.isHighSeason(LocalDate.of(2027, 1, 15)));
        assertTrue(Season.isHighSeason(LocalDate.of(2027, 12, 25)));

        assertFalse(Season.isHighSeason(LocalDate.of(2027, 3, 10)));
        assertFalse(Season.isHighSeason(LocalDate.of(2027, 11, 1)));
    }
}
