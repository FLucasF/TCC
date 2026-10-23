package com.pousadamarealta.api.mapper;

import com.pousadamarealta.api.dto.*;
import com.pousadamarealta.database.RoomDao;
import com.pousadamarealta.domain.Message;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.Room;

import java.time.format.DateTimeFormatter;

public class ApiMapper {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static RoomResponse toRoomResponse(Room room) {
        return new RoomResponse(
            room.getNumber(),
            room.getType().name(),
            room.getCapacity(),
            room.getNightlyRate().doubleValue()
        );
    }

    public static ReservationResponse toReservationResponse(Reservation reservation) {
        return new ReservationResponse(
            reservation.getId(),
            reservation.getRoomNumber(),
            reservation.getGuestName(),
            reservation.getGuestEmail(),
            reservation.getGuestCpf(),
            reservation.getCheckIn().format(DATE_FORMATTER),
            reservation.getCheckOut().format(DATE_FORMATTER),
            reservation.getGuests(),
            reservation.getRatePlan().name(),
            reservation.getNights(),
            reservation.getSubtotal().doubleValue(),
            reservation.getDiscount().doubleValue(),
            reservation.getTotal().doubleValue(),
            reservation.getStatus().name(),
            reservation.getAmountPaid().doubleValue(),
            reservation.getRefund() != null ? reservation.getRefund().doubleValue() : null
        );
    }

    public static DocumentResponse toDocumentResponse(Reservation reservation, RoomDao roomDao) {
        Room room = roomDao.findByNumber(reservation.getRoomNumber()).orElseThrow();
        DateTimeFormatter displayFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        return new DocumentResponse(
            "Comprovante de reserva",
            reservation.getId(),
            reservation.getGuestName(),
            reservation.getGuestCpf(),
            reservation.getRoomNumber(),
            room.getType().name(),
            reservation.getCheckIn().format(displayFormatter),
            reservation.getCheckOut().format(displayFormatter),
            reservation.getNights(),
            reservation.getSubtotal().doubleValue(),
            reservation.getDiscount().doubleValue(),
            reservation.getTotal().doubleValue()
        );
    }

    public static MessageResponse toMessageResponse(Message message) {
        return new MessageResponse(
            message.getId(),
            message.getChannel().name(),
            message.getTo(),
            message.getSubject(),
            message.getBody(),
            message.getStatus().name()
        );
    }
}
