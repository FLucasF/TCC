package com.pousadamarealta.web.mapper;

import com.pousadamarealta.domain.model.Message;
import com.pousadamarealta.domain.model.Reservation;
import com.pousadamarealta.domain.model.Room;
import com.pousadamarealta.web.dto.*;

public class DomainMapper {

    public static RoomDTO toDTO(Room room) {
        return new RoomDTO(
            room.getNumber(),
            room.getType().name(),
            room.getCapacity(),
            room.getNightlyRate().doubleValue()
        );
    }

    public static ReservationDTO toDTO(Reservation r) {
        ReservationDTO dto = new ReservationDTO();
        dto.setId(r.getId());
        dto.setRoomNumber(r.getRoomNumber());
        dto.setGuestName(r.getGuestName());
        dto.setGuestEmail(r.getGuestEmail());
        dto.setGuestCpf(r.getGuestCpf());
        dto.setCheckIn(r.getCheckIn().toString());
        dto.setCheckOut(r.getCheckOut().toString());
        dto.setGuests(r.getGuests());
        dto.setRatePlan(r.getRatePlan().name());
        dto.setNights(r.getNights());
        dto.setSubtotal(r.getSubtotal().doubleValue());
        dto.setDiscount(r.getDiscount().doubleValue());
        dto.setTotal(r.getTotal().doubleValue());
        dto.setStatus(r.getStatus().name());
        dto.setAmountPaid(r.getAmountPaid().doubleValue());

        if (r.getRefund() != null) {
            dto.setRefund(r.getRefund().doubleValue());
        }

        return dto;
    }

    public static DocumentDTO toDocumentDTO(Reservation r) {
        DocumentDTO dto = new DocumentDTO();
        dto.setTitle("Comprovante de reserva");
        dto.setReservationId(r.getId());
        dto.setGuestName(r.getGuestName());
        dto.setGuestCpf(r.getGuestCpf());
        dto.setRoomNumber(r.getRoomNumber());
        dto.setRoomType(getRoomTypeName(r.getRoomNumber()));
        dto.setCheckIn(r.getCheckIn().toString());
        dto.setCheckOut(r.getCheckOut().toString());
        dto.setNights(r.getNights());
        dto.setSubtotal(r.getSubtotal().doubleValue());
        dto.setDiscount(r.getDiscount().doubleValue());
        dto.setTotal(r.getTotal().doubleValue());
        return dto;
    }

    public static MessageDTO toDTO(Message m) {
        MessageDTO dto = new MessageDTO();
        dto.setId(m.getId());
        dto.setChannel(m.getChannel().name());
        dto.setTo(m.getTo());
        dto.setSubject(m.getSubject());
        dto.setBody(m.getBody());
        return dto;
    }

    private static String getRoomTypeName(String roomNumber) {
        return switch (roomNumber) {
            case "101", "102" -> "STANDARD";
            case "201", "202" -> "DELUXE";
            case "301" -> "SUITE";
            default -> "";
        };
    }
}
