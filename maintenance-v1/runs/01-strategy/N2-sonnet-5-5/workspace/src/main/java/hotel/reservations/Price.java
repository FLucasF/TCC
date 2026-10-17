package hotel.reservations;

import java.math.BigDecimal;

public record Price(int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {

    /** This price with a percentage taken off the subtotal, rounded to cents. */
    public Price withDiscountRate(BigDecimal rate) {
        BigDecimal discountAmount = PriceCalculator.cents(subtotal.multiply(rate));
        return new Price(nights, subtotal, discountAmount, subtotal.subtract(discountAmount));
    }
}
