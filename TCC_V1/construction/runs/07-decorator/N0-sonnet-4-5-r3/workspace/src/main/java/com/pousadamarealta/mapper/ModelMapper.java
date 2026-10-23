package com.pousadamarealta.mapper;

import com.pousadamarealta.dto.*;
import com.pousadamarealta.model.Message;
import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.model.Room;

import java.time.format.DateTimeFormatter;

public class ModelMapper {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    public static RoomDTO toDTO(Room room) {
        return new RoomDTO(
            room.getNumber(),
            room.getType().name(),
            room.getCapacity(),
            room.getNightlyRate()
        );
    }

    public static ReservationDTO toDTO(Reservation reservation) {
        ReservationDTO dto = new ReservationDTO();
        dto.setId(reservation.getId());
        dto.setRoomNumber(reservation.getRoomNumber());
        dto.setGuestName(reservation.getGuestName());
        dto.setGuestEmail(reservation.getGuestEmail());
        dto.setGuestCpf(reservation.getGuestCpf());
        dto.setCheckIn(reservation.getCheckIn().format(DATE_FORMATTER));
        dto.setCheckOut(reservation.getCheckOut().format(DATE_FORMATTER));
        dto.setGuests(reservation.getGuests());
        dto.setRatePlan(reservation.getRatePlan().name());
        dto.setNights(reservation.getNights());
        dto.setSubtotal(reservation.getSubtotal().doubleValue());
        dto.setDiscount(reservation.getDiscount().doubleValue());
        dto.setTotal(reservation.getTotal().doubleValue());
        dto.setStatus(reservation.getStatus().name());
        dto.setAmountPaid(reservation.getAmountPaid().doubleValue());
        dto.setRefund(reservation.getRefund() != null ? reservation.getRefund().doubleValue() : null);
        return dto;
    }

    public static DocumentDTO toDocumentDTO(Reservation reservation, String roomType) {
        DocumentDTO dto = new DocumentDTO();
        dto.setTitle("Comprovante de reserva");
        dto.setReservationId(reservation.getId());
        dto.setGuestName(reservation.getGuestName());
        dto.setGuestCpf(reservation.getGuestCpf());
        dto.setRoomNumber(reservation.getRoomNumber());
        dto.setRoomType(roomType);
        dto.setCheckIn(reservation.getCheckIn().format(DATE_FORMATTER));
        dto.setCheckOut(reservation.getCheckOut().format(DATE_FORMATTER));
        dto.setNights(reservation.getNights());
        dto.setSubtotal(reservation.getSubtotal().doubleValue());
        dto.setDiscount(reservation.getDiscount().doubleValue());
        dto.setTotal(reservation.getTotal().doubleValue());
        return dto;
    }

    public static MessageDTO toDTO(Message message) {
        MessageDTO dto = new MessageDTO();
        dto.setId(message.getId());
        dto.setChannel(message.getChannel().name());
        dto.setTo(message.getTo());
        dto.setSubject(message.getSubject());
        dto.setBody(message.getBody());
        dto.setStatus(message.getStatus().name());
        return dto;
    }
}
