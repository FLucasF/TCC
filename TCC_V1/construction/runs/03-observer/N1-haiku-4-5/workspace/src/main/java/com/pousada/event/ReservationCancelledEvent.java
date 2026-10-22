package com.pousada.event;

import com.pousada.model.Reservation;
import java.math.BigDecimal;

public class ReservationCancelledEvent extends ReservationEvent {
  private final boolean wasConfirmed;
  private final BigDecimal refund;

  public ReservationCancelledEvent(Reservation reservation, boolean wasConfirmed, BigDecimal refund) {
    super(reservation);
    this.wasConfirmed = wasConfirmed;
    this.refund = refund;
  }

  public boolean wasConfirmed() {
    return wasConfirmed;
  }

  public BigDecimal getRefund() {
    return refund;
  }
}
