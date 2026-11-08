package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.util.Map;

final class CorporateRatePlan extends StandardRatePlan {

    private static final Map<String, BigDecimal> CORPORATE_DISCOUNTS = Map.of(
            "TECNOSUL", new BigDecimal("0.12"),
            "AGROVALE", new BigDecimal("0.18"),
            "FARMAVIDA", new BigDecimal("0.25"));

    @Override
    public void validate(ReservationData data) throws HotelException {
        if (data.companyCode() == null || !CORPORATE_DISCOUNTS.containsKey(data.companyCode())) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST,
                    "company code must be one of: " + String.join(", ", CORPORATE_DISCOUNTS.keySet()));
        }
    }

    @Override
    public Price calculate(Room room, ReservationData data) {
        Price standardPrice = super.calculate(room, data);
        BigDecimal discount = cents(standardPrice.subtotal().multiply(getDiscount(data.companyCode())));
        return new Price(
                standardPrice.nights(),
                standardPrice.subtotal(),
                discount,
                standardPrice.subtotal().subtract(discount));
    }

    @Override
    public BigDecimal refundOnCancellation(Price price) {
        return price.total();
    }

    private BigDecimal getDiscount(String companyCode) {
        return CORPORATE_DISCOUNTS.getOrDefault(companyCode, BigDecimal.ZERO);
    }
}
