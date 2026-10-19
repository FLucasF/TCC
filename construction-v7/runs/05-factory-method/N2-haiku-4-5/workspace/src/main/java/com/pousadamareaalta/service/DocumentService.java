package com.pousadamareaalta.service;

import com.pousadamareaalta.document.*;
import com.pousadamareaalta.model.Document;
import com.pousadamareaalta.model.Reservation;
import com.pousadamareaalta.model.Room;

public class DocumentService {
    public Document generateDocument(Reservation reservation, Room room) {
        DocumentGenerator generator = selectGenerator(reservation);
        return generator.generate(reservation, room);
    }

    private DocumentGenerator selectGenerator(Reservation reservation) {
        if (reservation.getGuestCpf() != null) {
            return new CpfComprovante();
        } else if (reservation.getCompanyCnpj() != null) {
            return new CnpjFatura();
        } else if (reservation.getGuestPassport() != null) {
            return new PassportBookingReceipt();
        }
        throw new IllegalArgumentException("Unknown document type for reservation " + reservation.getId());
    }
}
