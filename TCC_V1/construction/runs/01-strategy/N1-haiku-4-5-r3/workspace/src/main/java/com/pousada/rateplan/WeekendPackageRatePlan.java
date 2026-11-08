package com.pousada.rateplan;

import com.pousada.model.Room;
import com.pousada.util.Money;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Map;

public class WeekendPackageRatePlan implements RatePlanStrategy {
  private static final Map<String, BigDecimal> PACKAGE_PRICES = Map.of(
      "STANDARD", BigDecimal.valueOf(520.00),
      "DELUXE", BigDecimal.valueOf(820.00),
      "SUITE", BigDecimal.valueOf(1200.00)
  );

  @Override
  public void validate(Room room, LocalDate checkIn, LocalDate checkOut, int guests, String companyCode)
      throws InvalidReservationException {
    if (checkIn.getDayOfWeek() != DayOfWeek.FRIDAY) {
      throw new InvalidReservationException("Weekend package must start on Friday", "INVALID_REQUEST");
    }

    if (checkOut.getDayOfWeek() != DayOfWeek.SUNDAY) {
      throw new InvalidReservationException("Weekend package must end on Sunday", "INVALID_REQUEST");
    }

    long days = java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
    if (days != 2) {
      throw new InvalidReservationException("Weekend package must be exactly 2 nights", "INVALID_REQUEST");
    }

    if (guests < 1 || guests > room.getCapacity()) {
      throw new InvalidReservationException("Invalid number of guests", "CAPACITY_EXCEEDED");
    }
  }

  @Override
  public PriceCalculation calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut) {
    int nights = 2;
    BigDecimal price = PACKAGE_PRICES.get(room.getType().toString());

    return new PriceCalculation(nights, price, BigDecimal.ZERO, price);
  }

  @Override
  public BigDecimal calculateRefund(BigDecimal amountPaid) {
    return Money.multiply(amountPaid, BigDecimal.valueOf(0.5));
  }
}
