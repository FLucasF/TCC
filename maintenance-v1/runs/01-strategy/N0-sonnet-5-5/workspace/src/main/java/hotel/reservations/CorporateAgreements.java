package hotel.reservations;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

/** The companies with an agreement with the inn, by company code, and the discount each one has. */
final class CorporateAgreements {

    private static final Map<String, BigDecimal> DISCOUNTS = Map.of(
            "TECNOSUL", new BigDecimal("0.12"),
            "AGROVALE", new BigDecimal("0.18"),
            "FARMAVIDA", new BigDecimal("0.25"));

    private CorporateAgreements() {
    }

    static Optional<BigDecimal> discountFor(String companyCode) {
        if (companyCode == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(DISCOUNTS.get(companyCode.trim()));
    }
}
