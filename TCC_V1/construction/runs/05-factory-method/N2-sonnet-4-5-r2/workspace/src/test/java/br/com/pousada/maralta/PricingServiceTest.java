package br.com.pousada.maralta;

import br.com.pousada.maralta.domain.RatePlan;
import br.com.pousada.maralta.domain.RoomType;
import br.com.pousada.maralta.service.PricingService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PricingServiceTest {

    private final PricingService pricingService = new PricingService();

    @Test
    void testNormalSeasonPrice() {
        LocalDate date = LocalDate.of(2027, 3, 10);
        BigDecimal price = pricingService.calculateNightPrice(date, RoomType.STANDARD);
        assertEquals(new BigDecimal("249.90"), price);
    }

    @Test
    void testHighSeasonPriceJuly() {
        LocalDate date = LocalDate.of(2027, 7, 10);
        BigDecimal price = pricingService.calculateNightPrice(date, RoomType.STANDARD);
        assertEquals(new BigDecimal("299.88"), price);
    }

    @Test
    void testHighSeasonPriceDecember() {
        LocalDate date = LocalDate.of(2027, 12, 20);
        BigDecimal price = pricingService.calculateNightPrice(date, RoomType.STANDARD);
        assertEquals(new BigDecimal("299.88"), price);
    }

    @Test
    void testNotHighSeasonEarlyDecember() {
        LocalDate date = LocalDate.of(2027, 12, 14);
        BigDecimal price = pricingService.calculateNightPrice(date, RoomType.STANDARD);
        assertEquals(new BigDecimal("249.90"), price);
    }

    @Test
    void testSubtotalCalculation() {
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);
        BigDecimal subtotal = pricingService.calculateSubtotal(checkIn, checkOut, RoomType.STANDARD);
        assertEquals(new BigDecimal("599.76"), subtotal);
    }

    @Test
    void testNonRefundableDiscount() {
        BigDecimal subtotal = new BigDecimal("779.80");
        BigDecimal discount = pricingService.calculateDiscount(subtotal, RatePlan.NON_REFUNDABLE);
        assertEquals(new BigDecimal("116.97"), discount);
    }

    @Test
    void testStandardNoDiscount() {
        BigDecimal subtotal = new BigDecimal("779.80");
        BigDecimal discount = pricingService.calculateDiscount(subtotal, RatePlan.STANDARD);
        assertEquals(new BigDecimal("0.00"), discount);
    }

    @Test
    void testTotalCalculation() {
        BigDecimal subtotal = new BigDecimal("779.80");
        BigDecimal discount = new BigDecimal("116.97");
        BigDecimal total = pricingService.calculateTotal(subtotal, discount);
        assertEquals(new BigDecimal("662.83"), total);
    }
}
