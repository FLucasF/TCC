package hotel.reservations;

import java.math.BigDecimal;
import java.util.Map;

/** Agreement with companies: each one has its own discount, and the guest must give the company code. */
final class CorporatePolicy extends FullRefundPolicy {

    static final Map<String, BigDecimal> DISCOUNTS = Map.of(
            "TECNOSUL", new BigDecimal("0.12"),
            "AGROVALE", new BigDecimal("0.18"),
            "FARMAVIDA", new BigDecimal("0.25"));

    @Override
    public Price price(Stay stay, Price fullPrice) {
        BigDecimal discount = stay.companyCode() == null ? null : DISCOUNTS.get(stay.companyCode());
        if (discount == null) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST, "the corporate plan needs the code of a company with an agreement");
        }
        return fullPrice.withDiscountRate(discount);
    }
}
