package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.util.Map;

/**
 * The agreement with companies: the guest gives the company's code and gets the discount agreed with it.
 * Everything paid comes back on cancellation.
 */
final class CorporatePlan implements RatePlanRules {

    static final Map<String, BigDecimal> DISCOUNT_BY_COMPANY = Map.of(
            "TECNOSUL", new BigDecimal("0.12"),
            "AGROVALE", new BigDecimal("0.18"),
            "FARMAVIDA", new BigDecimal("0.25"));

    @Override
    public void checkEligibility(NewReservation request) {
        discountOf(request.companyCode());
    }

    @Override
    public Price price(Room room, NewReservation request, PriceCalculator calculator) {
        return calculator.seasonalPrice(
                room, request.checkIn(), request.checkOut(), discountOf(request.companyCode()));
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return amountPaid;
    }

    private static BigDecimal discountOf(String companyCode) {
        BigDecimal discount = companyCode == null ? null : DISCOUNT_BY_COMPANY.get(companyCode);
        if (discount == null) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST, "the corporate rate needs the code of a company with an agreement");
        }
        return discount;
    }
}
