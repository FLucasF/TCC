package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

class StandardPlanStrategy implements RatePlanStrategy {

    private final PriceCalculator priceCalculator;

    StandardPlanStrategy(PriceCalculator priceCalculator) {
        this.priceCalculator = priceCalculator;
    }

    @Override
    public void validate(Room room, LocalDate checkIn, LocalDate checkOut, int nights, Optional<String> companyCode) {
    }

    @Override
    public Price calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut, Optional<String> companyCode) {
        return priceCalculator.calculateForRatePlan(room, checkIn, checkOut, RatePlan.STANDARD);
    }

    @Override
    public BigDecimal calculateRefund(Price price, ReservationStatus status) {
        return status == ReservationStatus.PENDING ? PriceCalculator.cents(BigDecimal.ZERO) : price.total();
    }
}
