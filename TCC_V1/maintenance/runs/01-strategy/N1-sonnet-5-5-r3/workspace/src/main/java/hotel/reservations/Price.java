package hotel.reservations;

import java.math.BigDecimal;

public record Price(int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {

    Price withDiscount(BigDecimal discount) {
        return new Price(nights, subtotal, discount, subtotal.subtract(discount));
    }
}
