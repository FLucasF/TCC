package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

final class CorporatePricing implements RatePlanPricing {

    private static final Map<String, BigDecimal> COMPANY_DISCOUNTS = Map.of(
            "TECNOSUL", new BigDecimal("0.12"),
            "AGROVALE", new BigDecimal("0.18"),
            "FARMAVIDA", new BigDecimal("0.25"));

    @Override
    public void validate(NewReservation request, Room room) {
        if (request.companyCode() == null || request.companyCode().isBlank()) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST, "corporate rate plan requires a company code");
        }
        if (!COMPANY_DISCOUNTS.containsKey(request.companyCode())) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST,
                    "company code " + request.companyCode() + " is not recognized");
        }
    }

    @Override
    public Price calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
        int nights = 0;
        BigDecimal subtotal = PriceCalculator.cents(BigDecimal.ZERO);
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            subtotal = subtotal.add(StandardPricing.nightPrice(room, night));
            nights++;
        }
        BigDecimal discountRate = COMPANY_DISCOUNTS.get(companyCode);
        BigDecimal discount = PriceCalculator.cents(subtotal.multiply(discountRate));
        return new Price(nights, subtotal, discount, subtotal.subtract(discount));
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
