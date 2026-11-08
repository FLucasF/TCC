package com.pousadamarealta.service.listeners;

import com.pousadamarealta.domain.DocumentFormatter;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.Room;
import com.pousadamarealta.domain.events.EventListener;
import com.pousadamarealta.domain.events.ReservationConfirmed;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.service.NotificationService;

public class GuestConfirmationListener implements EventListener<ReservationConfirmed> {
    private final NotificationService notificationService;
    private final RoomRepository roomRepository;

    public GuestConfirmationListener(NotificationService notificationService, RoomRepository roomRepository) {
        this.notificationService = notificationService;
        this.roomRepository = roomRepository;
    }

    @Override
    public void handle(ReservationConfirmed event) {
        Reservation reservation = event.reservation();
        Room room = roomRepository.findByNumber(reservation.getRoomNumber())
            .orElseThrow(() -> new RuntimeException("Room not found"));

        String subject = "Reserva confirmada - Pousada Maré Alta";
        String body = DocumentFormatter.formatConfirmationEmail(reservation, room.getType());

        notificationService.sendEmail(reservation.getGuestEmail(), subject, body);
    }
}
