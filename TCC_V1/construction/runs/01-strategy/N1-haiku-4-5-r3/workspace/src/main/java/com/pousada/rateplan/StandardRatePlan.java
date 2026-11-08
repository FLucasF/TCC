package com.pousada.rateplan;

import com.pousada.model.Room;
import com.pousada.util.Money;
import com.pousada.util.Season;

import java.math.BigDecimal;
import java.time.LocalDate;

public class StandardRatePlan implements RatePlanStrategy {
  @Override
  public void validate(Room room, LocalDate checkIn, LocalDate checkOut, int guests, String companyCode)
      throws InvalidReservationException {
    if (guests < 1 || guests > room.getCapacity()) {
      throw new InvalidReservationException("Invalid number of guests", "CAPACITY_EXCEEDED");
    }
  }

  @Override
  public PriceCalculation calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut) {
    int nights = (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
    BigDecimal subtotal = BigDecimal.ZERO;

    for (LocalDate date = checkIn; date.isBefore(checkOut); date = date.plusDays(1)) {
      BigDecimal nightRate = room.getNightlyRate();
      if (Season.isHighSeason(date)) {
        nightRate = Money.multiply(nightRate, BigDecimal.valueOf(1.2));
      }
      subtotal = Money.add(subtotal, nightRate);
    }

    return new PriceCalculation(nights, subtotal, BigDecimal.ZERO, subtotal);
  }

  @Override
  public BigDecimal calculateRefund(BigDecimal amountPaid) {
    return amountPaid;
  }
}
