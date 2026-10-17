package hotel.reservations;

import java.math.BigDecimal;

public record Price(int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {

    /** The same nights, with {@code rate} (e.g. 0.15 for 15%) taken off the subtotal, rounded to cents. */
    Price withDiscount(BigDecimal rate) {
        BigDecimal newDiscount = PriceCalculator.cents(subtotal.multiply(rate));
        return new Price(nights, subtotal, newDiscount, subtotal.subtract(newDiscount));
    }
}
