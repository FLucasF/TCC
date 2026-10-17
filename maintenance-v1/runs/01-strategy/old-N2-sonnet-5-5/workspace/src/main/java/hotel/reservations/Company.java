package hotel.reservations;

import java.math.BigDecimal;
import java.util.Optional;

/** The companies with a corporate agreement, and the discount each one negotiated. */
enum Company {
    TECNOSUL("0.12"),
    AGROVALE("0.18"),
    FARMAVIDA("0.25");

    private final BigDecimal discount;

    Company(String discount) {
        this.discount = new BigDecimal(discount);
    }

    BigDecimal discount() {
        return discount;
    }

    /** The company with this code (its name in capitals), if there is one. */
    static Optional<Company> withCode(String code) {
        for (Company company : values()) {
            if (company.name().equals(code)) {
                return Optional.of(company);
            }
        }
        return Optional.empty();
    }
}
