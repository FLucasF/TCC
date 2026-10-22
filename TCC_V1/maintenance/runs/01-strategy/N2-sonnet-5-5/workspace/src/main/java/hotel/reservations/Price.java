package hotel.reservations;

import java.math.BigDecimal;

public record Price(int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {

    /** The same nights with a discount of {@code rate} (0.15 is 15%) on the subtotal, rounded to cents. */
    Price withDiscount(BigDecimal rate) {
        BigDecimal discount = PriceCalculator.cents(subtotal.multiply(rate));
        return new Price(nights, subtotal, discount, subtotal.subtract(discount));
    }
}
