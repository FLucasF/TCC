package br.com.pousadamarealta;

import br.com.pousadamarealta.domain.RatePlan;
import br.com.pousadamarealta.domain.Reservation;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PricingTest {

    @Test
    void shouldCalculatePriceForHighSeason() {
        Reservation reservation = new Reservation(
            "101",
            "Test Guest",
            "test@example.com",
            "11122233344",
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12),
            2,
            RatePlan.STANDARD
        );

        BigDecimal baseRate = new BigDecimal("249.90");
        BigDecimal nightPrice = reservation.calculateNightPrice(LocalDate.of(2027, 7, 10), baseRate);

        assertEquals(new BigDecimal("299.88"), nightPrice);
    }

    @Test
    void shouldCalculateSubtotalForTwoNights() {
        Reservation reservation = new Reservation(
            "101",
            "Test Guest",
            "test@example.com",
            "11122233344",
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12),
            2,
            RatePlan.STANDARD
        );

        BigDecimal baseRate = new BigDecimal("249.90");
        BigDecimal subtotal = reservation.calculateSubtotal(baseRate);

        assertEquals(new BigDecimal("599.76"), subtotal);
    }

    @Test
    void shouldCalculateDiscountForNonRefundable() {
        Reservation reservation = new Reservation(
            "101",
            "Test Guest",
            "test@example.com",
            "11122233344",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            RatePlan.NON_REFUNDABLE
        );

        BigDecimal baseRate = new BigDecimal("389.90");
        BigDecimal subtotal = reservation.calculateSubtotal(baseRate);
        BigDecimal discount = reservation.calculateDiscount(subtotal);
        BigDecimal total = subtotal.subtract(discount);

        assertEquals(new BigDecimal("779.80"), subtotal);
        assertEquals(new BigDecimal("116.97"), discount);
        assertEquals(new BigDecimal("662.83"), total);
    }

    @Test
    void shouldHandleDecemberHighSeason() {
        Reservation reservation = new Reservation(
            "101",
            "Test Guest",
            "test@example.com",
            "11122233344",
            LocalDate.of(2027, 12, 14),
            LocalDate.of(2027, 12, 16),
            2,
            RatePlan.STANDARD
        );

        BigDecimal baseRate = new BigDecimal("249.90");

        BigDecimal night1Price = reservation.calculateNightPrice(LocalDate.of(2027, 12, 14), baseRate);
        BigDecimal night2Price = reservation.calculateNightPrice(LocalDate.of(2027, 12, 15), baseRate);

        assertEquals(new BigDecimal("249.90"), night1Price);
        assertEquals(new BigDecimal("299.88"), night2Price);
    }

    @Test
    void shouldFormatCpf() {
        Reservation reservation = new Reservation(
            "101",
            "Test Guest",
            "test@example.com",
            "11122233344",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            RatePlan.STANDARD
        );

        assertEquals("111.222.333-44", reservation.formatCpf());
    }

    @Test
    void shouldFormatMoney() {
        Reservation reservation = new Reservation(
            "101",
            "Test Guest",
            "test@example.com",
            "11122233344",
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            RatePlan.STANDARD
        );

        assertEquals("R$ 1.234,56", reservation.formatMoney(new BigDecimal("1234.56")));
    }
}
