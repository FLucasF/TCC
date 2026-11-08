package com.pousada.rateplan;

import com.pousada.model.Room;
import com.pousada.model.Reservation;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface RatePlanStrategy {
  void validate(Room room, LocalDate checkIn, LocalDate checkOut, int guests, String companyCode)
      throws InvalidReservationException;

  PriceCalculation calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut)
      throws InvalidReservationException;

  default PriceCalculation calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode)
      throws InvalidReservationException {
    return calculatePrice(room, checkIn, checkOut);
  }

  BigDecimal calculateRefund(BigDecimal amountPaid);

  public static class PriceCalculation {
    public final int nights;
    public final BigDecimal subtotal;
    public final BigDecimal discount;
    public final BigDecimal total;

    public PriceCalculation(int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
      this.nights = nights;
      this.subtotal = subtotal;
      this.discount = discount;
      this.total = total;
    }
  }

  public static class InvalidReservationException extends Exception {
    public final String errorCode;

    public InvalidReservationException(String message, String errorCode) {
      super(message);
      this.errorCode = errorCode;
    }
  }
}
