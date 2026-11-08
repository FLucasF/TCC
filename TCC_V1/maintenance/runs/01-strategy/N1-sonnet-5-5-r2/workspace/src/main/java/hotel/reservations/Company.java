package hotel.reservations;

import java.math.BigDecimal;
import java.util.Optional;

/** The companies with a corporate agreement, the code they give when booking and their discount. */
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

    /** The company with exactly this code, if there is one. */
    static Optional<Company> fromCode(String code) {
        for (Company company : values()) {
            if (company.name().equals(code)) {
                return Optional.of(company);
            }
        }
        return Optional.empty();
    }
}
