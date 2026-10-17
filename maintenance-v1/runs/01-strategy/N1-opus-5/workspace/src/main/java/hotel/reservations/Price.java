package hotel.reservations;

import java.math.BigDecimal;

public record Price(int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {

    /** The nights, with nothing taken off. */
    static Price withoutDiscount(int nights, BigDecimal subtotal) {
        return percentOff(nights, subtotal, BigDecimal.ZERO);
    }

    /** The nights with a share of them taken off, rounded to cents. */
    static Price percentOff(int nights, BigDecimal subtotal, BigDecimal share) {
        BigDecimal discount = PriceCalculator.cents(subtotal.multiply(share));
        return new Price(nights, subtotal, discount, subtotal.subtract(discount));
    }

    /** A closed price: the nights and the total are the price itself, with no discount. */
    static Price closed(int nights, BigDecimal amount) {
        BigDecimal price = PriceCalculator.cents(amount);
        return new Price(nights, price, PriceCalculator.cents(BigDecimal.ZERO), price);
    }
}
