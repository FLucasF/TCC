package com.pousadamarealta.service;

import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;

public interface ReportStrategy {
    String generate(ReservationRepository resRepo, RoomRepository roomRepo, Object param);
}
