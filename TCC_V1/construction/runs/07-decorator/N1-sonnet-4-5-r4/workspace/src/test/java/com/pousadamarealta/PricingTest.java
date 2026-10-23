package com.pousadamarealta;

import com.pousadamarealta.model.RatePlan;
import com.pousadamarealta.model.Reservation;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

public class PricingTest {

    @Test
    void testRegularSeasonPricing() {
        Reservation reservation = new Reservation();
        reservation.setCheckIn(LocalDate.of(2027, 3, 10));
        reservation.setCheckOut(LocalDate.of(2027, 3, 12));
        reservation.setRatePlan(RatePlan.STANDARD);

        BigDecimal baseRate = new BigDecimal("249.90");
        BigDecimal subtotal = reservation.calculateSubtotal(baseRate);

        assertThat(subtotal).isEqualByComparingTo(new BigDecimal("499.80"));
    }

    @Test
    void testHighSeasonPricing() {
        Reservation reservation = new Reservation();
        reservation.setCheckIn(LocalDate.of(2027, 7, 10));
        reservation.setCheckOut(LocalDate.of(2027, 7, 12));
        reservation.setRatePlan(RatePlan.STANDARD);

        BigDecimal baseRate = new BigDecimal("249.90");
        BigDecimal subtotal = reservation.calculateSubtotal(baseRate);

        assertThat(subtotal).isEqualByComparingTo(new BigDecimal("599.76"));
    }

    @Test
    void testNonRefundableDiscount() {
        Reservation reservation = new Reservation();
        reservation.setCheckIn(LocalDate.of(2027, 3, 10));
        reservation.setCheckOut(LocalDate.of(2027, 3, 12));
        reservation.setRatePlan(RatePlan.NON_REFUNDABLE);

        BigDecimal baseRate = new BigDecimal("249.90");
        BigDecimal subtotal = reservation.calculateSubtotal(baseRate);
        BigDecimal discount = reservation.calculateDiscount(subtotal);

        assertThat(subtotal).isEqualByComparingTo(new BigDecimal("499.80"));
        assertThat(discount).isEqualByComparingTo(new BigDecimal("74.97"));
    }

    @Test
    void testDecemberHighSeason() {
        Reservation reservation = new Reservation();
        reservation.setCheckIn(LocalDate.of(2027, 12, 15));
        reservation.setCheckOut(LocalDate.of(2027, 12, 17));
        reservation.setRatePlan(RatePlan.STANDARD);

        BigDecimal baseRate = new BigDecimal("249.90");
        BigDecimal subtotal = reservation.calculateSubtotal(baseRate);

        assertThat(subtotal).isEqualByComparingTo(new BigDecimal("599.76"));
    }

    @Test
    void testJanuaryHighSeason() {
        Reservation reservation = new Reservation();
        reservation.setCheckIn(LocalDate.of(2027, 1, 10));
        reservation.setCheckOut(LocalDate.of(2027, 1, 12));
        reservation.setRatePlan(RatePlan.STANDARD);

        BigDecimal baseRate = new BigDecimal("249.90");
        BigDecimal subtotal = reservation.calculateSubtotal(baseRate);

        assertThat(subtotal).isEqualByComparingTo(new BigDecimal("599.76"));
    }

    @Test
    void testNights() {
        Reservation reservation = new Reservation();
        reservation.setCheckIn(LocalDate.of(2027, 3, 10));
        reservation.setCheckOut(LocalDate.of(2027, 3, 12));

        assertThat(reservation.getNights()).isEqualTo(2);
    }
}
