package com.pousadamarealta;

import com.pousadamarealta.model.RatePlan;
import com.pousadamarealta.model.Room;
import com.pousadamarealta.model.RoomType;
import com.pousadamarealta.util.PriceCalculator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PriceCalculatorTest {

    @Test
    void shouldCalculateNormalSeasonPrice() {
        Room room = new Room("101", RoomType.STANDARD, 2, new BigDecimal("249.90"));
        LocalDate date = LocalDate.of(2027, 3, 10);

        BigDecimal price = PriceCalculator.calculateNightlyPrice(room, date);

        assertThat(price).isEqualByComparingTo("249.90");
    }

    @Test
    void shouldCalculateHighSeasonPriceInJuly() {
        Room room = new Room("101", RoomType.STANDARD, 2, new BigDecimal("249.90"));
        LocalDate date = LocalDate.of(2027, 7, 10);

        BigDecimal price = PriceCalculator.calculateNightlyPrice(room, date);

        assertThat(price).isEqualByComparingTo("299.88");
    }

    @Test
    void shouldCalculateHighSeasonPriceInDecember() {
        Room room = new Room("101", RoomType.STANDARD, 2, new BigDecimal("249.90"));
        LocalDate date = LocalDate.of(2027, 12, 15);

        BigDecimal price = PriceCalculator.calculateNightlyPrice(room, date);

        assertThat(price).isEqualByComparingTo("299.88");
    }

    @Test
    void shouldCalculateSubtotalForMultipleNights() {
        Room room = new Room("101", RoomType.STANDARD, 2, new BigDecimal("249.90"));
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        BigDecimal subtotal = PriceCalculator.calculateSubtotal(room, checkIn, checkOut);

        assertThat(subtotal).isEqualByComparingTo("599.76");
    }

    @Test
    void shouldCalculateDiscountForNonRefundable() {
        BigDecimal subtotal = new BigDecimal("599.76");

        BigDecimal discount = PriceCalculator.calculateDiscount(subtotal, RatePlan.NON_REFUNDABLE);

        assertThat(discount).isEqualByComparingTo("89.96");
    }

    @Test
    void shouldNotCalculateDiscountForStandard() {
        BigDecimal subtotal = new BigDecimal("599.76");

        BigDecimal discount = PriceCalculator.calculateDiscount(subtotal, RatePlan.STANDARD);

        assertThat(discount).isEqualByComparingTo("0.00");
    }

    @Test
    void shouldCalculateIss() {
        BigDecimal total = new BigDecimal("779.80");

        BigDecimal iss = PriceCalculator.calculateIss(total);

        assertThat(iss).isEqualByComparingTo("38.99");
    }

    @Test
    void shouldRoundToNearestEven() {
        BigDecimal value1 = new BigDecimal("10.125");
        BigDecimal value2 = new BigDecimal("10.135");

        assertThat(PriceCalculator.round(value1)).isEqualByComparingTo("10.12");
        assertThat(PriceCalculator.round(value2)).isEqualByComparingTo("10.14");
    }
}
