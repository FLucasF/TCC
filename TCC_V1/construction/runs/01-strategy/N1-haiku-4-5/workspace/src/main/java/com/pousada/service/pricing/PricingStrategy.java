package com.pousada.service.pricing;

import com.pousada.model.Room;
import java.math.BigDecimal;
import java.time.LocalDate;

public interface PricingStrategy {
    PricingResult calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut, int nights);
    RefundPolicy getRefundPolicy();
}
