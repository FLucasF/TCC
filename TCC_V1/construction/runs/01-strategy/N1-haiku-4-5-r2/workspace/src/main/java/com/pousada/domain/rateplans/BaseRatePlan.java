package com.pousada.domain.rateplans;

import com.pousada.domain.Money;
import com.pousada.domain.Room;
import com.pousada.domain.Season;
import java.math.BigDecimal;
import java.time.LocalDate;

public abstract class BaseRatePlan implements RatePlan {

    protected BigDecimal calculateNightlyPrice(Room room, LocalDate date) {
        BigDecimal basePrice = room.getNightlyRate();
        double seasonMultiplier = Season.getSeasonMultiplier(date);
        return Money.multiply(basePrice, seasonMultiplier);
    }

    protected BigDecimal calculateSubtotal(Room room, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal total = BigDecimal.ZERO;
        LocalDate current = checkIn;
        while (current.isBefore(checkOut)) {
            total = total.add(calculateNightlyPrice(room, current));
            current = current.plusDays(1);
        }
        return Money.round(total);
    }

    protected int calculateNights(LocalDate checkIn, LocalDate checkOut) {
        return (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    protected void validateBasic(int guests) {
        if (guests < 1) {
            throw new InvalidReservationException("INVALID_REQUEST", "Reserva deve ter pelo menos 1 hóspede");
        }
    }
}
