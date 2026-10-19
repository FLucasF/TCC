package com.pousadamarealta.service.report;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.Room;

import java.util.List;

public interface ReportGenerator {
    String generate(List<Reservation> reservations, List<Room> rooms, String parameter);
}
