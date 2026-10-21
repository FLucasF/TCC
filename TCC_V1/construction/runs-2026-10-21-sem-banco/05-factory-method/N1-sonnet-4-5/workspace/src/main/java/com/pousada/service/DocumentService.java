package com.pousada.service;

import com.pousada.domain.CnpjIdentification;
import com.pousada.domain.Document;
import com.pousada.domain.PassportIdentification;
import com.pousada.domain.Reservation;

import java.time.format.DateTimeFormatter;

public class DocumentService {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public Document generateDocument(Reservation reservation) {
        if (!reservation.isPaid()) {
            throw new IllegalStateException("Document can only be generated for paid reservations");
        }

        String title = getDocumentTitle(reservation);
        String checkIn = reservation.getCheckIn().format(DATE_FORMATTER);
        String checkOut = reservation.getCheckOut().format(DATE_FORMATTER);

        return new Document(
            title,
            reservation.getId(),
            reservation.getIdentification(),
            reservation.getRoomNumber(),
            reservation.getRoomType(),
            checkIn,
            checkOut,
            reservation.getNights(),
            reservation.getSubtotal(),
            reservation.getDiscount(),
            reservation.getTotal()
        );
    }

    private String getDocumentTitle(Reservation reservation) {
        if (reservation.getIdentification() instanceof CnpjIdentification) {
            return "Fatura de hospedagem";
        } else if (reservation.getIdentification() instanceof PassportIdentification) {
            return "Booking receipt";
        } else {
            return "Comprovante de reserva";
        }
    }
}
