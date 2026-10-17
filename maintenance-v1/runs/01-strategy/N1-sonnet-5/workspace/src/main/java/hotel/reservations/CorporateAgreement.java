package hotel.reservations;

import java.math.BigDecimal;
import java.util.Optional;

/** The companies with a negotiated discount under the CORPORATE rate plan. */
enum CorporateAgreement {
    TECNOSUL(new BigDecimal("0.12")),
    AGROVALE(new BigDecimal("0.18")),
    FARMAVIDA(new BigDecimal("0.25"));

    private final BigDecimal discount;

    CorporateAgreement(BigDecimal discount) {
        this.discount = discount;
    }

    BigDecimal discount() {
        return discount;
    }

    static Optional<CorporateAgreement> byCode(String companyCode) {
        if (companyCode == null) {
            return Optional.empty();
        }
        for (CorporateAgreement agreement : values()) {
            if (agreement.name().equals(companyCode)) {
                return Optional.of(agreement);
            }
        }
        return Optional.empty();
    }
}
