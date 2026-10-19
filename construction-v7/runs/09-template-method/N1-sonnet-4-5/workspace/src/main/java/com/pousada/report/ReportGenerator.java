package com.pousada.report;

import com.pousada.domain.model.Reservation;
import com.pousada.infrastructure.repository.ReservationRepository;
import com.pousada.infrastructure.repository.RoomRepository;

import java.util.List;

public interface ReportGenerator {
    String getName();
    String generate(String period, List<Reservation> reservations, RoomRepository roomRepository);
}
