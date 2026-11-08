package hotel.reservations;

import java.math.BigDecimal;
import java.util.Optional;

/** The companies with a corporate agreement; the code a guest informs is the company's name in capitals. */
public enum Company {
    TECNOSUL("0.12"),
    AGROVALE("0.18"),
    FARMAVIDA("0.25");

    private final BigDecimal discount;

    Company(String discount) {
        this.discount = new BigDecimal(discount);
    }

    public BigDecimal discount() {
        return discount;
    }

    public static Optional<Company> withCode(String code) {
        for (Company company : values()) {
            if (company.name().equals(code)) {
                return Optional.of(company);
            }
        }
        return Optional.empty();
    }
}
