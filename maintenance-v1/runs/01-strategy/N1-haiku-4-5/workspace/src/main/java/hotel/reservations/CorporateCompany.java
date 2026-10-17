package hotel.reservations;

import java.math.BigDecimal;
import java.util.Optional;

public enum CorporateCompany {
    TECNOSUL(new BigDecimal("0.12")),
    AGROVALE(new BigDecimal("0.18")),
    FARMAVIDA(new BigDecimal("0.25"));

    private final BigDecimal discountRate;

    CorporateCompany(BigDecimal discountRate) {
        this.discountRate = discountRate;
    }

    public BigDecimal discountRate() {
        return discountRate;
    }

    static Optional<CorporateCompany> findByCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(valueOf(code));
        } catch (IllegalArgumentException notFound) {
            return Optional.empty();
        }
    }
}
