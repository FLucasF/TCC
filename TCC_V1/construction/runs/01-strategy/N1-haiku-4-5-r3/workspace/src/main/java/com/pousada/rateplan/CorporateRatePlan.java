package com.pousada.rateplan;

import com.pousada.model.Room;
import com.pousada.util.Money;
import com.pousada.util.Season;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public class CorporateRatePlan implements RatePlanStrategy {
  private static final Map<String, Integer> COMPANY_DISCOUNTS = Map.of(
      "TECNOSUL", 12,
      "AGROVALE", 18,
      "FARMAVIDA", 25
  );

  @Override
  public void validate(Room room, LocalDate checkIn, LocalDate checkOut, int guests, String companyCode)
      throws InvalidReservationException {
    if (companyCode == null || companyCode.isBlank() || !COMPANY_DISCOUNTS.containsKey(companyCode)) {
      throw new InvalidReservationException("Invalid or missing company code", "INVALID_REQUEST");
    }

    if (guests < 1 || guests > room.getCapacity()) {
      throw new InvalidReservationException("Invalid number of guests", "CAPACITY_EXCEEDED");
    }
  }

  @Override
  public PriceCalculation calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
    int nights = (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
    BigDecimal subtotal = BigDecimal.ZERO;

    for (LocalDate date = checkIn; date.isBefore(checkOut); date = date.plusDays(1)) {
      BigDecimal nightRate = room.getNightlyRate();
      if (Season.isHighSeason(date)) {
        nightRate = Money.multiply(nightRate, BigDecimal.valueOf(1.2));
      }
      subtotal = Money.add(subtotal, nightRate);
    }

    int discountPercent = COMPANY_DISCOUNTS.get(companyCode);
    BigDecimal discount = Money.percentage(subtotal, discountPercent);
    BigDecimal total = Money.subtract(subtotal, discount);

    return new PriceCalculation(nights, subtotal, discount, total);
  }

  @Override
  public PriceCalculation calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut)
      throws InvalidReservationException {
    throw new InvalidReservationException("Corporate plan requires company code", "INVALID_REQUEST");
  }

  @Override
  public BigDecimal calculateRefund(BigDecimal amountPaid) {
    return amountPaid;
  }
}
