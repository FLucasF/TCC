package com.pousadamarealta;

import com.pousadamarealta.domain.RatePlan;
import com.pousadamarealta.domain.Room;
import com.pousadamarealta.domain.RoomType;
import com.pousadamarealta.service.PricingService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

public class PricingTest {

    @Test
    public void testHighSeasonPricing() {
        PricingService service = new PricingService();
        Room room = new Room("101", RoomType.STANDARD, 2, new BigDecimal("249.90"));

        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        BigDecimal subtotal = service.calculateSubtotal(room, checkIn, checkOut);

        assertThat(subtotal).isEqualByComparingTo("599.76");
    }

    @Test
    public void testRegularSeasonPricing() {
        PricingService service = new PricingService();
        Room room = new Room("101", RoomType.STANDARD, 2, new BigDecimal("249.90"));

        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        BigDecimal subtotal = service.calculateSubtotal(room, checkIn, checkOut);

        assertThat(subtotal).isEqualByComparingTo("499.80");
    }

    @Test
    public void testNonRefundableDiscount() {
        PricingService service = new PricingService();

        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal discount = service.calculateDiscount(subtotal, RatePlan.NON_REFUNDABLE);

        assertThat(discount).isEqualByComparingTo("89.96");
    }

    @Test
    public void testStandardNoDiscount() {
        PricingService service = new PricingService();

        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal discount = service.calculateDiscount(subtotal, RatePlan.STANDARD);

        assertThat(discount).isEqualByComparingTo("0.00");
    }
}
