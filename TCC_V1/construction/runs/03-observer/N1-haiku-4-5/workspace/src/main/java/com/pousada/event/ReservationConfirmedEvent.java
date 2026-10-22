package com.pousada.event;

import com.pousada.model.Reservation;
import java.math.BigDecimal;

public class ReservationConfirmedEvent extends ReservationEvent {
  private final BigDecimal amountPaid;

  public ReservationConfirmedEvent(Reservation reservation, BigDecimal amountPaid) {
    super(reservation);
    this.amountPaid = amountPaid;
  }

  public BigDecimal getAmountPaid() {
    return amountPaid;
  }
}
