package com.pousadamarealta.api;

import java.math.BigDecimal;

public class ConfirmReservationRequest {
    public BigDecimal amountPaid;

    public ConfirmReservationRequest() {
    }

    public ConfirmReservationRequest(BigDecimal amountPaid) {
        this.amountPaid = amountPaid;
    }
}
