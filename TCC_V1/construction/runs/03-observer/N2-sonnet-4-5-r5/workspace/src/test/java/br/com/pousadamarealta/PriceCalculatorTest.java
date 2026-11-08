package br.com.pousadamarealta;

import br.com.pousadamarealta.domain.PriceCalculator;
import br.com.pousadamarealta.domain.RatePlan;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PriceCalculatorTest {

    @Test
    void shouldCalculateRegularSeasonPrice() {
        LocalDate date = LocalDate.of(2027, 3, 10);
        BigDecimal baseRate = new BigDecimal("249.90");

        BigDecimal price = PriceCalculator.calculateNightPrice(date, baseRate);

        assertEquals(new BigDecimal("249.90"), price);
    }

    @Test
    void shouldCalculateHighSeasonPriceInJuly() {
        LocalDate date = LocalDate.of(2027, 7, 10);
        BigDecimal baseRate = new BigDecimal("249.90");

        BigDecimal price = PriceCalculator.calculateNightPrice(date, baseRate);

        assertEquals(new BigDecimal("299.88"), price);
    }

    @Test
    void shouldCalculateHighSeasonPriceInDecember() {
        LocalDate date = LocalDate.of(2027, 12, 20);
        BigDecimal baseRate = new BigDecimal("249.90");

        BigDecimal price = PriceCalculator.calculateNightPrice(date, baseRate);

        assertEquals(new BigDecimal("299.88"), price);
    }

    @Test
    void shouldCalculateHighSeasonPriceInJanuary() {
        LocalDate date = LocalDate.of(2027, 1, 15);
        BigDecimal baseRate = new BigDecimal("249.90");

        BigDecimal price = PriceCalculator.calculateNightPrice(date, baseRate);

        assertEquals(new BigDecimal("299.88"), price);
    }

    @Test
    void shouldCalculateSubtotalForTwoNights() {
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);
        BigDecimal baseRate = new BigDecimal("249.90");

        BigDecimal subtotal = PriceCalculator.calculateSubtotal(checkIn, checkOut, baseRate);

        assertEquals(new BigDecimal("599.76"), subtotal);
    }

    @Test
    void shouldCalculateNonRefundableDiscount() {
        BigDecimal subtotal = new BigDecimal("779.80");

        BigDecimal discount = PriceCalculator.calculateDiscount(subtotal, RatePlan.NON_REFUNDABLE);

        assertEquals(new BigDecimal("116.97"), discount);
    }

    @Test
    void shouldNotApplyDiscountForStandardPlan() {
        BigDecimal subtotal = new BigDecimal("779.80");

        BigDecimal discount = PriceCalculator.calculateDiscount(subtotal, RatePlan.STANDARD);

        assertEquals(new BigDecimal("0.00"), discount);
    }

    @Test
    void shouldCalculateTotal() {
        BigDecimal subtotal = new BigDecimal("779.80");
        BigDecimal discount = new BigDecimal("116.97");

        BigDecimal total = PriceCalculator.calculateTotal(subtotal, discount);

        assertEquals(new BigDecimal("662.83"), total);
    }
}
