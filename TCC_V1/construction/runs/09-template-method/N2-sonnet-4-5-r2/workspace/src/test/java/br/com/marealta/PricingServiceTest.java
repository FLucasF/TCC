package br.com.marealta;

import br.com.marealta.domain.RatePlan;
import br.com.marealta.service.PricingService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PricingServiceTest {
    private final PricingService pricingService = new PricingService();

    @Test
    void shouldCalculateRegularSeasonNightPrice() {
        BigDecimal baseRate = new BigDecimal("249.90");
        LocalDate date = LocalDate.of(2027, 3, 10);

        BigDecimal price = pricingService.calculateNightPrice(baseRate, date);

        assertThat(price).isEqualByComparingTo("249.90");
    }

    @Test
    void shouldCalculateHighSeasonNightPriceInJuly() {
        BigDecimal baseRate = new BigDecimal("249.90");
        LocalDate date = LocalDate.of(2027, 7, 10);

        BigDecimal price = pricingService.calculateNightPrice(baseRate, date);

        assertThat(price).isEqualByComparingTo("299.88");
    }

    @Test
    void shouldCalculateHighSeasonNightPriceInDecember() {
        BigDecimal baseRate = new BigDecimal("249.90");
        LocalDate date = LocalDate.of(2027, 12, 20);

        BigDecimal price = pricingService.calculateNightPrice(baseRate, date);

        assertThat(price).isEqualByComparingTo("299.88");
    }

    @Test
    void shouldCalculateHighSeasonNightPriceInJanuary() {
        BigDecimal baseRate = new BigDecimal("249.90");
        LocalDate date = LocalDate.of(2027, 1, 15);

        BigDecimal price = pricingService.calculateNightPrice(baseRate, date);

        assertThat(price).isEqualByComparingTo("299.88");
    }

    @Test
    void shouldCalculateSubtotalForTwoNightsInHighSeason() {
        BigDecimal baseRate = new BigDecimal("249.90");
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        BigDecimal subtotal = pricingService.calculateSubtotal(baseRate, checkIn, checkOut);

        assertThat(subtotal).isEqualByComparingTo("599.76");
    }

    @Test
    void shouldCalculateSubtotalForTwoNightsInRegularSeason() {
        BigDecimal baseRate = new BigDecimal("389.90");
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        BigDecimal subtotal = pricingService.calculateSubtotal(baseRate, checkIn, checkOut);

        assertThat(subtotal).isEqualByComparingTo("779.80");
    }

    @Test
    void shouldCalculateDiscountForNonRefundable() {
        BigDecimal subtotal = new BigDecimal("599.76");
        RatePlan ratePlan = RatePlan.NON_REFUNDABLE;

        BigDecimal discount = pricingService.calculateDiscount(subtotal, ratePlan);

        assertThat(discount).isEqualByComparingTo("89.96");
    }

    @Test
    void shouldCalculateNoDiscountForStandard() {
        BigDecimal subtotal = new BigDecimal("599.76");
        RatePlan ratePlan = RatePlan.STANDARD;

        BigDecimal discount = pricingService.calculateDiscount(subtotal, ratePlan);

        assertThat(discount).isEqualByComparingTo("0.00");
    }

    @Test
    void shouldCalculateTotalWithDiscount() {
        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal discount = new BigDecimal("89.96");

        BigDecimal total = pricingService.calculateTotal(subtotal, discount);

        assertThat(total).isEqualByComparingTo("509.80");
    }

    @Test
    void shouldCalculateTotalWithoutDiscount() {
        BigDecimal subtotal = new BigDecimal("779.80");
        BigDecimal discount = BigDecimal.ZERO;

        BigDecimal total = pricingService.calculateTotal(subtotal, discount);

        assertThat(total).isEqualByComparingTo("779.80");
    }
}
