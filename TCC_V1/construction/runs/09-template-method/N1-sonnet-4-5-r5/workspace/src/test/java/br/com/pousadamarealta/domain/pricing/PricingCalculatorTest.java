package br.com.pousadamarealta.domain.pricing;

import br.com.pousadamarealta.domain.reservation.RatePlan;
import br.com.pousadamarealta.domain.room.Room;
import br.com.pousadamarealta.domain.room.RoomType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PricingCalculatorTest {

    @Test
    public void testCalculateNightlyRateHighSeason() {
        Room room = new Room("101", RoomType.STANDARD);
        LocalDate date = LocalDate.of(2027, 7, 10); // Julho = alta temporada

        BigDecimal rate = PricingCalculator.calculateNightlyRate(room, date);
        // 249.90 * 1.20 = 299.88
        assertEquals(new BigDecimal("299.88"), rate);
    }

    @Test
    public void testCalculateNightlyRateLowSeason() {
        Room room = new Room("101", RoomType.STANDARD);
        LocalDate date = LocalDate.of(2027, 3, 10); // Março = baixa temporada

        BigDecimal rate = PricingCalculator.calculateNightlyRate(room, date);
        assertEquals(new BigDecimal("249.90"), rate);
    }

    @Test
    public void testCalculateSubtotalTwoNightsHighSeason() {
        Room room = new Room("101", RoomType.STANDARD);
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        BigDecimal subtotal = PricingCalculator.calculateSubtotal(room, checkIn, checkOut);
        // 2 noites * 299.88 = 599.76
        assertEquals(new BigDecimal("599.76"), subtotal);
    }

    @Test
    public void testCalculateDiscountNonRefundable() {
        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal discount = PricingCalculator.calculateDiscount(subtotal, RatePlan.NON_REFUNDABLE);
        // 599.76 * 0.15 = 89.964 -> arredonda para 89.96
        assertEquals(new BigDecimal("89.96"), discount);
    }

    @Test
    public void testCalculateDiscountStandard() {
        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal discount = PricingCalculator.calculateDiscount(subtotal, RatePlan.STANDARD);
        assertEquals(BigDecimal.ZERO.setScale(2), discount);
    }

    @Test
    public void testCalculateTotal() {
        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal discount = new BigDecimal("89.96");
        BigDecimal total = PricingCalculator.calculateTotal(subtotal, discount);
        assertEquals(new BigDecimal("509.80"), total);
    }
}
