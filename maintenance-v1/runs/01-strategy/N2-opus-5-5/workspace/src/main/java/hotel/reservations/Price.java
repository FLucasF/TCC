package hotel.reservations;

import java.math.BigDecimal;

public record Price(int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {

    /** The same nights with a share of the subtotal taken off (rounded to cents). */
    Price withDiscount(BigDecimal share) {
        BigDecimal discountAmount = PriceCalculator.cents(subtotal.multiply(share));
        return new Price(nights, subtotal, discountAmount, subtotal.subtract(discountAmount));
    }
}
