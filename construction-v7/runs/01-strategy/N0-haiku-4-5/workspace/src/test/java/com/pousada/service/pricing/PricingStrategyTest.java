package com.pousada.service.pricing;

import com.pousada.domain.Room;
import com.pousada.exception.ApiException;
import com.pousada.service.pricing.*;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.Assert.*;

public class PricingStrategyTest {

    @Test
    public void testStandardPricingNoHighSeason() throws ApiException {
        Room room = new Room(101, "STANDARD", 2, new BigDecimal("249.90"));
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        PricingStrategy strategy = new StandardPricingStrategy();
        PricingStrategy.PricingResult result = strategy.calculatePrice(room, checkIn, checkOut, 2, null);

        assertEquals(new BigDecimal("499.80"), result.subtotal);
        assertEquals(BigDecimal.ZERO.compareTo(result.discount), 0);
        assertEquals(new BigDecimal("499.80"), result.total);
    }

    @Test
    public void testStandardPricingHighSeason() throws ApiException {
        Room room = new Room(101, "STANDARD", 2, new BigDecimal("249.90"));
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        PricingStrategy strategy = new StandardPricingStrategy();
        PricingStrategy.PricingResult result = strategy.calculatePrice(room, checkIn, checkOut, 2, null);

        assertEquals(new BigDecimal("599.76"), result.subtotal);
        assertEquals(BigDecimal.ZERO.compareTo(result.discount), 0);
        assertEquals(new BigDecimal("599.76"), result.total);
    }

    @Test
    public void testNonRefundablePricing() throws ApiException {
        Room room = new Room(101, "STANDARD", 2, new BigDecimal("249.90"));
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        PricingStrategy strategy = new NonRefundablePricingStrategy();
        PricingStrategy.PricingResult result = strategy.calculatePrice(room, checkIn, checkOut, 2, null);

        assertEquals(new BigDecimal("499.80"), result.subtotal);
        assertEquals(new BigDecimal("74.97"), result.discount);
        assertEquals(new BigDecimal("424.83"), result.total);
    }

    @Test
    public void testCorporatePricingTecnosul() throws ApiException {
        Room room = new Room(201, "DELUXE", 3, new BigDecimal("389.90"));
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        PricingStrategy strategy = new CorporatePricingStrategy();
        PricingStrategy.PricingResult result = strategy.calculatePrice(room, checkIn, checkOut, 2, "TECNOSUL");

        assertEquals(new BigDecimal("779.80"), result.subtotal);
        assertEquals(new BigDecimal("93.58"), result.discount);
        assertEquals(new BigDecimal("686.22"), result.total);
    }

    @Test
    public void testCorporatePricingInvalid() {
        Room room = new Room(201, "DELUXE", 3, new BigDecimal("389.90"));
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        PricingStrategy strategy = new CorporatePricingStrategy();
        assertThrows(ApiException.class, () ->
                strategy.calculatePrice(room, checkIn, checkOut, 2, "INVALID")
        );
    }

    @Test
    public void testWeekendPackagePricing() throws ApiException {
        Room room = new Room(201, "DELUXE", 3, new BigDecimal("389.90"));
        LocalDate checkIn = LocalDate.of(2027, 3, 12); // Friday
        LocalDate checkOut = LocalDate.of(2027, 3, 14); // Sunday

        PricingStrategy strategy = new WeekendPackagePricingStrategy();
        PricingStrategy.PricingResult result = strategy.calculatePrice(room, checkIn, checkOut, 2, null);

        assertEquals(new BigDecimal("820.00"), result.subtotal);
        assertEquals(BigDecimal.ZERO.compareTo(result.discount), 0);
        assertEquals(new BigDecimal("820.00"), result.total);
    }

    @Test
    public void testWeekendPackageInvalidDate() {
        Room room = new Room(201, "DELUXE", 3, new BigDecimal("389.90"));
        LocalDate checkIn = LocalDate.of(2027, 3, 10); // Wednesday
        LocalDate checkOut = LocalDate.of(2027, 3, 12); // Friday

        PricingStrategy strategy = new WeekendPackagePricingStrategy();
        assertThrows(ApiException.class, () ->
                strategy.calculatePrice(room, checkIn, checkOut, 2, null)
        );
    }

    @Test
    public void testLongStayPricing() throws ApiException {
        Room room = new Room(101, "STANDARD", 2, new BigDecimal("249.90"));
        LocalDate checkIn = LocalDate.of(2027, 3, 1);
        LocalDate checkOut = LocalDate.of(2027, 3, 8);

        PricingStrategy strategy = new LongStayPricingStrategy();
        PricingStrategy.PricingResult result = strategy.calculatePrice(room, checkIn, checkOut, 7, null);

        assertEquals(new BigDecimal("1749.30"), result.subtotal);
        assertEquals(new BigDecimal("174.93"), result.discount);
        assertEquals(new BigDecimal("1574.37"), result.total);
    }

    @Test
    public void testLongStayInvalidNights() {
        Room room = new Room(101, "STANDARD", 2, new BigDecimal("249.90"));
        LocalDate checkIn = LocalDate.of(2027, 3, 1);
        LocalDate checkOut = LocalDate.of(2027, 3, 7);

        PricingStrategy strategy = new LongStayPricingStrategy();
        assertThrows(ApiException.class, () ->
                strategy.calculatePrice(room, checkIn, checkOut, 6, null)
        );
    }
}
