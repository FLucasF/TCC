package com.pousada.strategy;

import com.pousada.domain.Reservation;
import com.pousada.domain.Room;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public interface ReportStrategy {
    String generate(List<Reservation> reservations, List<Room> rooms, Object parameter);
    String getReportName();
}
