package pousada;

import org.junit.jupiter.api.Test;
import pousada.domain.RatePlan;
import pousada.domain.Room;
import pousada.domain.RoomType;
import pousada.pricing.PricingCalculator;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PricingCalculatorTest {

    @Test
    void testHighSeasonPricing() {
        Room room = new Room("101", RoomType.STANDARD);
        PricingCalculator calculator = new PricingCalculator();

        PricingCalculator.PricingResult result = calculator.calculate(
            room,
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12),
            RatePlan.STANDARD
        );

        assertThat(result.getNights()).isEqualTo(2);
        assertThat(result.getSubtotal()).isEqualByComparingTo(new BigDecimal("599.76"));
        assertThat(result.getDiscount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getTotal()).isEqualByComparingTo(new BigDecimal("599.76"));
    }

    @Test
    void testNonRefundableDiscount() {
        Room room = new Room("201", RoomType.DELUXE);
        PricingCalculator calculator = new PricingCalculator();

        PricingCalculator.PricingResult result = calculator.calculate(
            room,
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            RatePlan.NON_REFUNDABLE
        );

        assertThat(result.getNights()).isEqualTo(2);
        assertThat(result.getSubtotal()).isEqualByComparingTo(new BigDecimal("779.80"));
        assertThat(result.getDiscount()).isEqualByComparingTo(new BigDecimal("116.97"));
        assertThat(result.getTotal()).isEqualByComparingTo(new BigDecimal("662.83"));
    }

    @Test
    void testLowSeasonPricing() {
        Room room = new Room("102", RoomType.STANDARD);
        PricingCalculator calculator = new PricingCalculator();

        PricingCalculator.PricingResult result = calculator.calculate(
            room,
            LocalDate.of(2027, 3, 15),
            LocalDate.of(2027, 3, 17),
            RatePlan.STANDARD
        );

        assertThat(result.getNights()).isEqualTo(2);
        assertThat(result.getSubtotal()).isEqualByComparingTo(new BigDecimal("499.80"));
        assertThat(result.getDiscount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getTotal()).isEqualByComparingTo(new BigDecimal("499.80"));
    }

    @Test
    void testDecemberHighSeason() {
        Room room = new Room("301", RoomType.SUITE);
        PricingCalculator calculator = new PricingCalculator();

        PricingCalculator.PricingResult result = calculator.calculate(
            room,
            LocalDate.of(2027, 12, 15),
            LocalDate.of(2027, 12, 16),
            RatePlan.STANDARD
        );

        assertThat(result.getNights()).isEqualTo(1);
        assertThat(result.getSubtotal()).isEqualByComparingTo(new BigDecimal("695.88"));
        assertThat(result.getTotal()).isEqualByComparingTo(new BigDecimal("695.88"));
    }

    @Test
    void testDecemberBeforeHighSeason() {
        Room room = new Room("301", RoomType.SUITE);
        PricingCalculator calculator = new PricingCalculator();

        PricingCalculator.PricingResult result = calculator.calculate(
            room,
            LocalDate.of(2027, 12, 14),
            LocalDate.of(2027, 12, 15),
            RatePlan.STANDARD
        );

        assertThat(result.getNights()).isEqualTo(1);
        assertThat(result.getSubtotal()).isEqualByComparingTo(new BigDecimal("579.90"));
        assertThat(result.getTotal()).isEqualByComparingTo(new BigDecimal("579.90"));
    }
}
