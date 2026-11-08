package br.com.marealta.pricing;

import br.com.marealta.domain.RatePlan;
import br.com.marealta.domain.Room;
import br.com.marealta.domain.RoomType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PricingServiceTest {
    private final PricingService pricingService = new PricingService();

    @Test
    void shouldCalculateNightPriceInLowSeason() {
        Room room = new Room("101", RoomType.STANDARD, 2, new BigDecimal("249.90"));
        LocalDate night = LocalDate.of(2027, 3, 10);

        BigDecimal price = pricingService.calculateNightPrice(room, night);

        assertThat(price).isEqualByComparingTo("249.90");
    }

    @Test
    void shouldCalculateNightPriceInHighSeasonJuly() {
        Room room = new Room("101", RoomType.STANDARD, 2, new BigDecimal("249.90"));
        LocalDate night = LocalDate.of(2027, 7, 10);

        BigDecimal price = pricingService.calculateNightPrice(room, night);

        assertThat(price).isEqualByComparingTo("299.88");
    }

    @Test
    void shouldCalculateNightPriceInHighSeasonDecember() {
        Room room = new Room("101", RoomType.STANDARD, 2, new BigDecimal("249.90"));
        LocalDate night = LocalDate.of(2027, 12, 20);

        BigDecimal price = pricingService.calculateNightPrice(room, night);

        assertThat(price).isEqualByComparingTo("299.88");
    }

    @Test
    void shouldCalculateSubtotalForTwoNightsInHighSeason() {
        Room room = new Room("101", RoomType.STANDARD, 2, new BigDecimal("249.90"));
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        BigDecimal subtotal = pricingService.calculateSubtotal(room, checkIn, checkOut);

        assertThat(subtotal).isEqualByComparingTo("599.76");
    }

    @Test
    void shouldCalculateNonRefundableDiscount() {
        BigDecimal subtotal = new BigDecimal("599.76");

        BigDecimal discount = pricingService.calculateDiscount(subtotal, RatePlan.NON_REFUNDABLE);

        assertThat(discount).isEqualByComparingTo("89.96");
    }

    @Test
    void shouldNotCalculateDiscountForStandardPlan() {
        BigDecimal subtotal = new BigDecimal("599.76");

        BigDecimal discount = pricingService.calculateDiscount(subtotal, RatePlan.STANDARD);

        assertThat(discount).isEqualByComparingTo("0.00");
    }

    @Test
    void shouldCalculateTotal() {
        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal discount = new BigDecimal("89.96");

        BigDecimal total = pricingService.calculateTotal(subtotal, discount);

        assertThat(total).isEqualByComparingTo("509.80");
    }
}
