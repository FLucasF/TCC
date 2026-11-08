package hotel.reservations;

import java.math.BigDecimal;
import java.util.Map;

/** The discount depends on the company the guest has an agreement through, named by its code. */
final class CorporateRule extends DiscountedRule {

    private static final Map<String, BigDecimal> DISCOUNT_BY_COMPANY = Map.of(
            "TECNOSUL", new BigDecimal("0.12"),
            "AGROVALE", new BigDecimal("0.18"),
            "FARMAVIDA", new BigDecimal("0.25"));

    @Override
    protected BigDecimal discountRate(NewReservation request) {
        BigDecimal discount = request.companyCode() == null ? null : DISCOUNT_BY_COMPANY.get(request.companyCode());
        if (discount == null) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST, "the corporate rate needs the code of a company with an agreement");
        }
        return discount;
    }

    @Override
    public BigDecimal refund(Reservation reservation) {
        return reservation.amountPaid();
    }
}
