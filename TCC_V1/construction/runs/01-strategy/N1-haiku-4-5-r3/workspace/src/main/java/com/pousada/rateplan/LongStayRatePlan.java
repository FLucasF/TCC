package com.pousada.rateplan;

import com.pousada.model.Room;
import com.pousada.util.Money;
import com.pousada.util.Season;

import java.math.BigDecimal;
import java.time.LocalDate;

public class LongStayRatePlan implements RatePlanStrategy {
  private static final int MIN_NIGHTS = 7;
  private static final int DISCOUNT_PERCENT = 10;

  @Override
  public void validate(Room room, LocalDate checkIn, LocalDate checkOut, int guests, String companyCode)
      throws InvalidReservationException {
    int nights = (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
    if (nights < MIN_NIGHTS) {
      throw new InvalidReservationException("Long stay requires at least 7 nights", "INVALID_REQUEST");
    }

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

    BigDecimal discount = Money.percentage(subtotal, DISCOUNT_PERCENT);
    BigDecimal total = Money.subtract(subtotal, discount);

    return new PriceCalculation(nights, subtotal, discount, total);
  }

  @Override
  public BigDecimal calculateRefund(BigDecimal amountPaid) {
    return amountPaid;
  }
}
