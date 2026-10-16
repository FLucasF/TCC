package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.util.Map;

final class CorporatePolicy implements RatePlanPolicy {

    private static final Map<String, BigDecimal> COMPANY_DISCOUNTS = Map.of(
            "TECNOSUL", new BigDecimal("0.12"),
            "AGROVALE", new BigDecimal("0.18"),
            "FARMAVIDA", new BigDecimal("0.25"));

    private final PriceCalculator priceCalculator;

    CorporatePolicy(PriceCalculator priceCalculator) {
        this.priceCalculator = priceCalculator;
    }

    @Override
    public void validate(NewReservation request, Room room) {
        if (discountFor(request.companyCode()) == null) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST,
                    "the corporate rate requires a known companyCode (TECNOSUL, AGROVALE or FARMAVIDA)");
        }
    }

    @Override
    public Price calculate(Room room, NewReservation request) {
        PriceCalculator.NightlySubtotal nightly =
                priceCalculator.nightlySubtotal(room, request.checkIn(), request.checkOut());
        BigDecimal discount = PriceCalculator.cents(nightly.subtotal().multiply(discountFor(request.companyCode())));
        return new Price(nightly.nights(), nightly.subtotal(), discount, nightly.subtotal().subtract(discount));
    }

    @Override
    public BigDecimal refund(Reservation reservation) {
        return reservation.amountPaid();
    }

    private static BigDecimal discountFor(String companyCode) {
        return companyCode == null ? null : COMPANY_DISCOUNTS.get(companyCode);
    }
}
