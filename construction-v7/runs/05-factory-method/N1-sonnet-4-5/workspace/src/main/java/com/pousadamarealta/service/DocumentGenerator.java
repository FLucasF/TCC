package com.pousadamarealta.service;

import com.pousadamarealta.domain.Document;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.Room;

import java.math.BigDecimal;

public interface DocumentGenerator {
    Document generate(Reservation reservation, Room room, BigDecimal subtotal, BigDecimal discount, BigDecimal total);
}
