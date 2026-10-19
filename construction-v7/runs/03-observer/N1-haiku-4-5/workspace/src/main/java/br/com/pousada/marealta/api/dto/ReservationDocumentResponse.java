package br.com.pousada.marealta.api.dto;

import br.com.pousada.marealta.model.ReservationDocument;
import java.time.LocalDate;

public record ReservationDocumentResponse(
        String title,
        long reservationId,
        String guestName,
        String guestCpf,
        int roomNumber,
        String roomType,
        LocalDate checkIn,
        LocalDate checkOut,
        int nights,
        double subtotal,
        double discount,
        double total
) {
    public static ReservationDocumentResponse from(ReservationDocument document) {
        return new ReservationDocumentResponse(
                document.title(),
                document.reservationId(),
                document.guestName(),
                document.guestCpf(),
                document.roomNumber(),
                document.roomType(),
                document.checkIn(),
                document.checkOut(),
                document.nights(),
                document.subtotal(),
                document.discount(),
                document.total()
        );
    }
}
