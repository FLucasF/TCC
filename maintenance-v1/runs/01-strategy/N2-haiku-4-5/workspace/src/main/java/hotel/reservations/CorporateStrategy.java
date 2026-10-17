package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

final class CorporateStrategy implements RatePlanStrategy {

    private static final Map<String, BigDecimal> COMPANY_DISCOUNTS = Map.ofEntries(
            Map.entry("TECNOSUL", new BigDecimal("0.12")),
            Map.entry("AGROVALE", new BigDecimal("0.18")),
            Map.entry("FARMAVIDA", new BigDecimal("0.25")));

    private final PriceCalculator priceCalculator;
    private final String companyCode;

    CorporateStrategy(PriceCalculator priceCalculator, String companyCode) {
        if (!COMPANY_DISCOUNTS.containsKey(companyCode)) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST, "company code '" + companyCode + "' is not valid");
        }
        this.priceCalculator = priceCalculator;
        this.companyCode = companyCode;
    }

    @Override
    public Price calculate(Room room, LocalDate checkIn, LocalDate checkOut) {
        int nights = 0;
        BigDecimal subtotal = PriceCalculator.cents(BigDecimal.ZERO);
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            subtotal = subtotal.add(priceCalculator.nightPrice(room, night));
            nights++;
        }
        BigDecimal discountRate = COMPANY_DISCOUNTS.get(companyCode);
        BigDecimal discount = PriceCalculator.cents(subtotal.multiply(discountRate));
        return new Price(nights, subtotal, discount, subtotal.subtract(discount));
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return amountPaid;
    }

    @Override
    public void validate(LocalDate checkIn, LocalDate checkOut, int nights) {
    }
}
