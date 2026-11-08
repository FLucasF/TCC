package com.pousadamarealta;

import org.junit.jupiter.api.Test;
import com.pousadamarealta.model.RatePlan;
import com.pousadamarealta.util.PriceCalculator;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

public class ReservationServiceTest {

    @Test
    public void shouldCalculateSubtotalForHighSeason() {
        BigDecimal baseRate = new BigDecimal("249.90");
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        BigDecimal subtotal = PriceCalculator.calculateSubtotal(baseRate, checkIn, checkOut);

        assertThat(subtotal).isEqualTo(new BigDecimal("599.76"));
    }

    @Test
    public void shouldCalculateNonRefundableDiscount() {
        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal discount = PriceCalculator.calculateDiscount(subtotal, RatePlan.NON_REFUNDABLE);

        assertThat(discount).isEqualTo(new BigDecimal("89.96"));
    }

    @Test
    public void shouldCalculateTotalAfterDiscount() {
        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal discount = new BigDecimal("89.96");
        BigDecimal total = PriceCalculator.calculateTotal(subtotal, discount);

        assertThat(total).isEqualTo(new BigDecimal("509.80"));
    }

    @Test
    public void shouldNotApplyDiscountToStandardPlan() {
        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal discount = PriceCalculator.calculateDiscount(subtotal, RatePlan.STANDARD);

        assertThat(discount).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    public void shouldDetectHighSeasonInJuly() {
        LocalDate date = LocalDate.of(2027, 7, 15);
        assertThat(PriceCalculator.isHighSeason(date)).isTrue();
    }

    @Test
    public void shouldNotDetectHighSeasonBeforeDay15InDecember() {
        LocalDate date = LocalDate.of(2027, 12, 14);
        assertThat(PriceCalculator.isHighSeason(date)).isFalse();
    }

    @Test
    public void shouldDetectHighSeasonFromDay15InDecember() {
        LocalDate date = LocalDate.of(2027, 12, 15);
        assertThat(PriceCalculator.isHighSeason(date)).isTrue();
    }

    @Test
    public void shouldDetectHighSeasonInJanuary() {
        LocalDate date = LocalDate.of(2027, 1, 10);
        assertThat(PriceCalculator.isHighSeason(date)).isTrue();
    }

    @Test
    public void shouldDetectHighSeasonInFebruary() {
        LocalDate date = LocalDate.of(2027, 2, 15);
        assertThat(PriceCalculator.isHighSeason(date)).isTrue();
    }

    @Test
    public void shouldNotDetectHighSeasonInMarch() {
        LocalDate date = LocalDate.of(2027, 3, 15);
        assertThat(PriceCalculator.isHighSeason(date)).isFalse();
    }

    @Test
    public void shouldNotDetectHighSeasonInNovember() {
        LocalDate date = LocalDate.of(2027, 11, 15);
        assertThat(PriceCalculator.isHighSeason(date)).isFalse();
    }
}
