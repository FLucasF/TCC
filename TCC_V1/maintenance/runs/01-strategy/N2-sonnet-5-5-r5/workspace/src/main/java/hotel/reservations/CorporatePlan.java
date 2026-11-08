package hotel.reservations;

import java.math.BigDecimal;
import java.util.Map;

/** The discount agreed with a company, chosen by the company code the guest gives. */
final class CorporatePlan extends DiscountedPlan {

    private static final Map<String, BigDecimal> DISCOUNT_BY_COMPANY = Map.of(
            "TECNOSUL", new BigDecimal("0.12"),
            "AGROVALE", new BigDecimal("0.18"),
            "FARMAVIDA", new BigDecimal("0.25"));

    @Override
    protected BigDecimal discountRate(NewReservation request, Price regularPrice) {
        BigDecimal discount = request.companyCode() == null ? null : DISCOUNT_BY_COMPANY.get(request.companyCode());
        if (discount == null) {
            throw new HotelException(ErrorCode.INVALID_REQUEST, "the corporate rate needs the code of a partner company");
        }
        return discount;
    }
}
