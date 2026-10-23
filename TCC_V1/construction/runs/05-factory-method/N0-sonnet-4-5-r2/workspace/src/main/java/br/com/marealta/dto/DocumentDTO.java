package br.com.marealta.dto;

import br.com.marealta.model.GuestIdentification;
import br.com.marealta.model.Reservation;
import br.com.marealta.model.Room;

public record DocumentDTO(
    String title,
    long reservationId,
    String guestName,
    String guestCpf,
    String companyCnpj,
    String companyName,
    String guestPassport,
    String guestCountry,
    String roomNumber,
    String roomType,
    String checkIn,
    String checkOut,
    int nights,
    double subtotal,
    double discount,
    double total,
    String text
) {
    public static DocumentDTO from(Reservation reservation, Room room, String documentText) {
        GuestIdentification id = reservation.getGuestIdentification();

        String title = switch (id) {
            case GuestIdentification.IndividualGuest ig -> "Comprovante de reserva";
            case GuestIdentification.CorporateGuest cg -> "Fatura de hospedagem";
            case GuestIdentification.ForeignGuest fg -> "Booking receipt";
        };

        return new DocumentDTO(
            title,
            reservation.getId(),
            reservation.getGuestName(),
            id instanceof GuestIdentification.IndividualGuest ig ? ig.cpf() : null,
            id instanceof GuestIdentification.CorporateGuest cg ? cg.cnpj() : null,
            id instanceof GuestIdentification.CorporateGuest cg ? cg.companyName() : null,
            id instanceof GuestIdentification.ForeignGuest fg ? fg.passport() : null,
            id instanceof GuestIdentification.ForeignGuest fg ? fg.country() : null,
            reservation.getRoomNumber(),
            room.getType().name(),
            reservation.getCheckIn().toString(),
            reservation.getCheckOut().toString(),
            reservation.getNights(),
            reservation.getSubtotal().doubleValue(),
            reservation.getDiscount().doubleValue(),
            reservation.getTotal().doubleValue(),
            documentText
        );
    }
}
