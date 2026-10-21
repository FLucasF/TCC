package hotel.reservations;

import hotel.rooms.Room;
import java.time.LocalDate;

/** Kept for compatibility; delegates to rate plan strategies. */
public final class PriceCalculator {

    public Price calculate(Room room, LocalDate checkIn, LocalDate checkOut, RatePlan ratePlan) {
        Guest dummyGuest = new Guest("", "", "");
        NewReservation request = new NewReservation(room.number(), dummyGuest, checkIn, checkOut, 1, ratePlan, null);
        RatePlanStrategy strategy = RatePlanStrategies.strategyFor(ratePlan);
        return strategy.calculatePrice(request, room);
    }
}
