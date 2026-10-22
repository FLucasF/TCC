package com.pousadamareaalta.api;

import com.pousadamareaalta.api.dto.*;
import com.pousadamareaalta.model.Message;
import com.pousadamareaalta.model.Reservation;
import com.pousadamareaalta.model.Room;
import com.pousadamareaalta.service.RoomService;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

public class ApiMapper {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final RoomService roomService;

    public ApiMapper(RoomService roomService) {
        this.roomService = roomService;
    }

    public RoomDTO toRoomDTO(Room room) {
        return new RoomDTO(
            room.getNumber(),
            room.getType().name(),
            room.getCapacity(),
            room.getNightlyRate()
        );
    }

    public ReservationDTO toReservationDTO(Reservation r) {
        return new ReservationDTO(
            r.getId(),
            r.getRoomNumber(),
            r.getGuestName(),
            r.getGuestEmail(),
            r.getGuestCpf(),
            r.getCheckIn().format(DATE_FORMATTER),
            r.getCheckOut().format(DATE_FORMATTER),
            r.getGuests(),
            r.getRatePlan().name(),
            r.getNights(),
            r.getSubtotal(),
            r.getDiscount(),
            r.getTotal(),
            r.getStatus().name(),
            r.getAmountPaid(),
            r.getRefund()
        );
    }

    public ReceiptDTO toReceiptDTO(Reservation r) {
        Room room = roomService.getRoom(r.getRoomNumber());

        return new ReceiptDTO(
            "Comprovante de reserva",
            r.getId(),
            r.getGuestName(),
            r.getGuestCpf(),
            r.getRoomNumber(),
            room.getType().name(),
            r.getCheckIn().format(DATE_FORMATTER),
            r.getCheckOut().format(DATE_FORMATTER),
            r.getNights(),
            r.getSubtotal(),
            r.getDiscount(),
            r.getTotal()
        );
    }

    public MessageDTO toMessageDTO(Message m) {
        return new MessageDTO(
            m.getId(),
            m.getChannel(),
            m.getTo(),
            m.getSubject(),
            m.getBody()
        );
    }
}
