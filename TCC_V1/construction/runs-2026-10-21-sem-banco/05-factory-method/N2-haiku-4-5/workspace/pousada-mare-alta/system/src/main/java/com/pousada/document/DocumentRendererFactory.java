package com.pousada.document;

import com.pousada.domain.Reservation;
import com.pousada.service.RoomService;

public class DocumentRendererFactory {
    private final RoomService roomService;

    public DocumentRendererFactory(RoomService roomService) {
        this.roomService = roomService;
    }

    public DocumentRenderer getRenderer(Reservation reservation) {
        var room = roomService.getRoom(reservation.getRoomNumber());
        if (reservation.getGuestCpf() != null) {
            return new CpfReceiptRenderer(room);
        } else if (reservation.getCompanyCnpj() != null) {
            return new CnpjInvoiceRenderer(room);
        } else if (reservation.getGuestPassport() != null) {
            return new PassportReceiptRenderer(room);
        } else {
            throw new IllegalArgumentException("Reservation must have CPF, CNPJ, or Passport");
        }
    }
}
