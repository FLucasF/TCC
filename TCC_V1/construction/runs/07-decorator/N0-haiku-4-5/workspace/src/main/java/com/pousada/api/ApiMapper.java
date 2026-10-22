package com.pousada.api;

import com.pousada.api.dto.MessageResponse;
import com.pousada.api.dto.ReceiptResponse;
import com.pousada.api.dto.ReservationResponse;
import com.pousada.api.dto.RoomResponse;
import com.pousada.models.Message;
import com.pousada.models.Receipt;
import com.pousada.models.Reservation;
import com.pousada.models.Room;
import com.pousada.utils.FormatterUtil;

public class ApiMapper {
    public static RoomResponse mapRoom(Room room) {
        return new RoomResponse(room.getNumber(), room.getType(), room.getCapacity(), room.getNightlyRate());
    }

    public static ReservationResponse mapReservation(Reservation reservation) {
        ReservationResponse res = new ReservationResponse();
        res.setId(reservation.getId());
        res.setRoomNumber(reservation.getRoomNumber());
        res.setGuestName(reservation.getGuestName());
        res.setGuestEmail(reservation.getGuestEmail());
        res.setGuestCpf(reservation.getGuestCpf());
        res.setCheckIn(FormatterUtil.formatDateISO(reservation.getCheckIn()));
        res.setCheckOut(FormatterUtil.formatDateISO(reservation.getCheckOut()));
        res.setGuests(reservation.getGuests());
        res.setRatePlan(reservation.getRatePlan());
        res.setNights(reservation.getNights());
        res.setSubtotal(reservation.getSubtotal());
        res.setDiscount(reservation.getDiscount());
        res.setTotal(reservation.getTotal());
        res.setStatus(reservation.getStatus());
        res.setAmountPaid(reservation.getAmountPaid());
        res.setRefund(reservation.getRefund());
        return res;
    }

    public static MessageResponse mapMessage(Message message) {
        return new MessageResponse(
            message.getId(),
            message.getChannel(),
            message.getTo(),
            message.getSubject(),
            message.getBody(),
            message.getStatus()
        );
    }

    public static ReceiptResponse mapReceipt(Receipt receipt) {
        ReceiptResponse res = new ReceiptResponse();
        res.setTitle(receipt.getTitle());
        res.setReservationId(receipt.getReservationId());
        res.setGuestName(receipt.getGuestName());
        res.setGuestCpf(receipt.getGuestCpf());
        res.setRoomNumber(receipt.getRoomNumber());
        res.setRoomType(receipt.getRoomType());
        res.setCheckIn(FormatterUtil.formatDateISO(receipt.getCheckIn()));
        res.setCheckOut(FormatterUtil.formatDateISO(receipt.getCheckOut()));
        res.setNights(receipt.getNights());
        res.setSubtotal(receipt.getSubtotal());
        res.setDiscount(receipt.getDiscount());
        res.setTotal(receipt.getTotal());
        return res;
    }
}
