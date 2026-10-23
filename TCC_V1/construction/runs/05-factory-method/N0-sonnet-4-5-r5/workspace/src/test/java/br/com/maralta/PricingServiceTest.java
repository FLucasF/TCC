package br.com.maralta;

import br.com.maralta.model.RatePlan;
import br.com.maralta.model.Room;
import br.com.maralta.model.RoomType;
import br.com.maralta.service.PricingService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class PricingServiceTest {

    private final PricingService pricingService = new PricingService();

    @Test
    void testLowSeasonNightlyRate() {
        Room room = new Room("101", RoomType.STANDARD);
        LocalDate lowSeasonDate = LocalDate.of(2027, 3, 10);

        BigDecimal rate = pricingService.calculateNightlyRate(room, lowSeasonDate);

        assertEquals(new BigDecimal("249.90"), rate);
    }

    @Test
    void testHighSeasonNightlyRateInJuly() {
        Room room = new Room("101", RoomType.STANDARD);
        LocalDate julyDate = LocalDate.of(2027, 7, 15);

        BigDecimal rate = pricingService.calculateNightlyRate(room, julyDate);

        assertEquals(new BigDecimal("299.88"), rate);
    }

    @Test
    void testHighSeasonNightlyRateInDecember() {
        Room room = new Room("101", RoomType.STANDARD);
        LocalDate decemberDate = LocalDate.of(2027, 12, 20);

        BigDecimal rate = pricingService.calculateNightlyRate(room, decemberDate);

        assertEquals(new BigDecimal("299.88"), rate);
    }

    @Test
    void testSubtotalCalculation() {
        Room room = new Room("201", RoomType.DELUXE);
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        BigDecimal subtotal = pricingService.calculateSubtotal(room, checkIn, checkOut);

        assertEquals(new BigDecimal("779.80"), subtotal);
    }

    @Test
    void testNonRefundableDiscount() {
        BigDecimal subtotal = new BigDecimal("779.80");

        BigDecimal discount = pricingService.calculateDiscount(subtotal, RatePlan.NON_REFUNDABLE);

        assertEquals(new BigDecimal("116.97"), discount);
    }

    @Test
    void testStandardPlanNoDiscount() {
        BigDecimal subtotal = new BigDecimal("779.80");

        BigDecimal discount = pricingService.calculateDiscount(subtotal, RatePlan.STANDARD);

        assertEquals(BigDecimal.ZERO, discount);
    }

    @Test
    void testTotalCalculation() {
        BigDecimal subtotal = new BigDecimal("779.80");
        BigDecimal discount = new BigDecimal("116.97");

        BigDecimal total = pricingService.calculateTotal(subtotal, discount);

        assertEquals(new BigDecimal("662.83"), total);
    }
}
