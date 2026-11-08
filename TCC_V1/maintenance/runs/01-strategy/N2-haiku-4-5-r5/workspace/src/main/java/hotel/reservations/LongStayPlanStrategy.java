package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

class LongStayPlanStrategy implements RatePlanStrategy {

    private static final BigDecimal LONG_STAY_DISCOUNT = new BigDecimal("0.10");

    private final PriceCalculator priceCalculator;

    LongStayPlanStrategy(PriceCalculator priceCalculator) {
        this.priceCalculator = priceCalculator;
    }

    @Override
    public void validate(Room room, LocalDate checkIn, LocalDate checkOut, int nights, Optional<String> companyCode) {
        if (nights < 7) {
            throw new HotelException(ErrorCode.INVALID_REQUEST, "long stay must be at least 7 nights");
        }
    }

    @Override
    public Price calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut, Optional<String> companyCode) {
        return priceCalculator.calculateWithDiscount(room, checkIn, checkOut, LONG_STAY_DISCOUNT);
    }

    @Override
    public BigDecimal calculateRefund(Price price, ReservationStatus status) {
        return status == ReservationStatus.PENDING ? PriceCalculator.cents(BigDecimal.ZERO) : price.total();
    }
}
