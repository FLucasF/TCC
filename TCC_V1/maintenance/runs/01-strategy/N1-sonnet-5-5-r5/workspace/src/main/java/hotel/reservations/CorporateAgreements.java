package hotel.reservations;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

/** The companies that have an agreement with the inn, by code, and the discount each one has. */
final class CorporateAgreements {

    private static final Map<String, BigDecimal> DISCOUNTS = Map.of(
            "TECNOSUL", new BigDecimal("0.12"),
            "AGROVALE", new BigDecimal("0.18"),
            "FARMAVIDA", new BigDecimal("0.25"));

    private CorporateAgreements() {
    }

    static Optional<BigDecimal> discountFor(String companyCode) {
        return Optional.ofNullable(companyCode).map(DISCOUNTS::get);
    }
}
