package br.com.marealto.service;

import br.com.marealto.model.RatePlan;
import br.com.marealto.model.Room;
import br.com.marealto.model.RoomType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PricingServiceTest {
    private PricingService pricingService;
    private Room standardRoom;

    @BeforeEach
    void setUp() {
        pricingService = new PricingService();
        standardRoom = new Room("101", RoomType.STANDARD, 2, new BigDecimal("249.90"));
    }

    @Test
    void testHighSeasonDetection() {
        assertTrue(pricingService.isHighSeason(LocalDate.of(2027, 12, 15)));
        assertTrue(pricingService.isHighSeason(LocalDate.of(2027, 12, 31)));
        assertTrue(pricingService.isHighSeason(LocalDate.of(2027, 1, 15)));
        assertTrue(pricingService.isHighSeason(LocalDate.of(2027, 2, 28)));
        assertTrue(pricingService.isHighSeason(LocalDate.of(2027, 7, 1)));
        assertTrue(pricingService.isHighSeason(LocalDate.of(2027, 7, 31)));

        assertFalse(pricingService.isHighSeason(LocalDate.of(2027, 12, 14)));
        assertFalse(pricingService.isHighSeason(LocalDate.of(2027, 3, 1)));
        assertFalse(pricingService.isHighSeason(LocalDate.of(2027, 6, 30)));
        assertFalse(pricingService.isHighSeason(LocalDate.of(2027, 8, 1)));
    }

    @Test
    void testNormalSeasonPrice() {
        BigDecimal price = pricingService.calculateNightPrice(standardRoom, LocalDate.of(2027, 3, 10));
        assertEquals(new BigDecimal("249.90"), price);
    }

    @Test
    void testHighSeasonPrice() {
        BigDecimal price = pricingService.calculateNightPrice(standardRoom, LocalDate.of(2027, 7, 10));
        assertEquals(new BigDecimal("299.88"), price);
    }

    @Test
    void testSubtotalCalculation() {
        BigDecimal subtotal = pricingService.calculateSubtotal(
                standardRoom,
                LocalDate.of(2027, 7, 10),
                LocalDate.of(2027, 7, 12)
        );
        assertEquals(new BigDecimal("599.76"), subtotal);
    }

    @Test
    void testNonRefundableDiscount() {
        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal discount = pricingService.calculateDiscount(subtotal, RatePlan.NON_REFUNDABLE);
        assertEquals(new BigDecimal("89.96"), discount);
    }

    @Test
    void testStandardNoDiscount() {
        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal discount = pricingService.calculateDiscount(subtotal, RatePlan.STANDARD);
        assertEquals(new BigDecimal("0.00"), discount);
    }

    @Test
    void testTotalCalculation() {
        BigDecimal total = pricingService.calculateTotal(new BigDecimal("599.76"), new BigDecimal("89.96"));
        assertEquals(new BigDecimal("509.80"), total);
    }

    @Test
    void testIssCalculation() {
        BigDecimal iss = pricingService.calculateIss(new BigDecimal("779.80"));
        assertEquals(new BigDecimal("38.99"), iss);
    }
}
