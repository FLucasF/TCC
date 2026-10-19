package com.pousadamarealta;

import com.pousadamarealta.domain.RatePlan;
import com.pousadamarealta.domain.Reservation;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ReservationTest {

    @Test
    void testCalculateSubtotalHighSeason() {
        // 10/07/2027 a 12/07/2027 - 2 noites em alta temporada
        Reservation reservation = new Reservation(
                1, 101, "Test", "test@test.com", "12345678901",
                LocalDate.of(2027, 7, 10),
                LocalDate.of(2027, 7, 12),
                1, RatePlan.STANDARD
        );

        BigDecimal nightlyRate = new BigDecimal("249.90");
        BigDecimal subtotal = reservation.calculateSubtotal(nightlyRate);

        // Alta temporada = 249.90 * 1.20 = 299.88 por noite
        // 2 noites = 599.76
        assertEquals(new BigDecimal("599.76"), subtotal);
    }

    @Test
    void testCalculateDiscountNonRefundable() {
        Reservation reservation = new Reservation(
                1, 101, "Test", "test@test.com", "12345678901",
                LocalDate.of(2027, 3, 10),
                LocalDate.of(2027, 3, 12),
                1, RatePlan.NON_REFUNDABLE
        );

        BigDecimal nightlyRate = new BigDecimal("249.90");
        BigDecimal subtotal = reservation.calculateSubtotal(nightlyRate);
        BigDecimal discount = reservation.calculateDiscount(subtotal);

        // 15% de desconto sobre 499.80 = 74.97
        assertEquals(new BigDecimal("74.97"), discount);
    }

    @Test
    void testCancelRefundStandard() {
        Reservation reservation = new Reservation(
                1, 101, "Test", "test@test.com", "12345678901",
                LocalDate.of(2027, 3, 10),
                LocalDate.of(2027, 3, 12),
                1, RatePlan.STANDARD
        );

        BigDecimal nightlyRate = new BigDecimal("249.90");
        reservation.confirm(nightlyRate);
        BigDecimal amountPaid = reservation.getAmountPaid();

        reservation.cancel();

        assertEquals(amountPaid, reservation.getRefund());
    }

    @Test
    void testCancelNoRefundNonRefundable() {
        Reservation reservation = new Reservation(
                1, 101, "Test", "test@test.com", "12345678901",
                LocalDate.of(2027, 3, 10),
                LocalDate.of(2027, 3, 12),
                1, RatePlan.NON_REFUNDABLE
        );

        BigDecimal nightlyRate = new BigDecimal("249.90");
        reservation.confirm(nightlyRate);

        reservation.cancel();

        assertEquals(BigDecimal.ZERO, reservation.getRefund());
    }
}
