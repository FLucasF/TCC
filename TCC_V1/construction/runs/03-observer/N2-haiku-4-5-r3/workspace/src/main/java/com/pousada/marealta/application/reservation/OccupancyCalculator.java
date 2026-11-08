package com.pousada.marealta.application.reservation;

import com.pousada.marealta.domain.repository.ReservationRepository;
import com.pousada.marealta.domain.reservation.Reservation;
import com.pousada.marealta.domain.room.Room;
import com.pousada.marealta.domain.shared.DateRange;

public class OccupancyCalculator {
    private final ReservationRepository reservationRepository;

    public OccupancyCalculator(ReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    public void validateOccupancy(Room room, DateRange dates, int guestCount) {
        if (guestCount < 1) {
            throw new IllegalArgumentException("At least 1 guest required");
        }

        if (guestCount > room.capacity()) {
            throw new IllegalArgumentException("Guest count exceeds room capacity");
        }

        var conflicts = reservationRepository.findByRoomAndDateRange(room.number(), dates);
        if (!conflicts.isEmpty()) {
            throw new IllegalArgumentException("Room already booked for some nights");
        }
    }
}
