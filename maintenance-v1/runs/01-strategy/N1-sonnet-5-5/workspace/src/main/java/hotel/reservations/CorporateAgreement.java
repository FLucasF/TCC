package hotel.reservations;

import java.math.BigDecimal;

/** The companies with an agreement with the inn, each with its discount on the nightly rates. */
enum CorporateAgreement {
    TECNOSUL("0.12"),
    AGROVALE("0.18"),
    FARMAVIDA("0.25");

    private final BigDecimal discount;

    CorporateAgreement(String discount) {
        this.discount = new BigDecimal(discount);
    }

    BigDecimal discount() {
        return discount;
    }

    /** The agreement for a company code, which is the company's name in upper case. */
    static CorporateAgreement forCode(String companyCode) {
        for (CorporateAgreement agreement : values()) {
            if (agreement.name().equals(companyCode)) {
                return agreement;
            }
        }
        throw new HotelException(
                ErrorCode.INVALID_REQUEST, "the CORPORATE plan needs the code of a company with an agreement");
    }
}
