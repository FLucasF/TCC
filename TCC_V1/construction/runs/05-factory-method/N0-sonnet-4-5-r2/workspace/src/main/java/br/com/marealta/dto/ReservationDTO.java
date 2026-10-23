package br.com.marealta.dto;

import br.com.marealta.model.GuestIdentification;
import br.com.marealta.model.Reservation;

public record ReservationDTO(
    long id,
    String roomNumber,
    String guestName,
    String guestEmail,
    String guestCpf,
    String companyCnpj,
    String companyName,
    String guestPassport,
    String guestCountry,
    String checkIn,
    String checkOut,
    int guests,
    String ratePlan,
    int nights,
    double subtotal,
    double discount,
    double total,
    String status,
    double amountPaid,
    Double refund
) {
    public static ReservationDTO from(Reservation reservation) {
        GuestIdentification id = reservation.getGuestIdentification();

        return new ReservationDTO(
            reservation.getId(),
            reservation.getRoomNumber(),
            reservation.getGuestName(),
            reservation.getGuestEmail(),
            id instanceof GuestIdentification.IndividualGuest ig ? ig.cpf() : null,
            id instanceof GuestIdentification.CorporateGuest cg ? cg.cnpj() : null,
            id instanceof GuestIdentification.CorporateGuest cg ? cg.companyName() : null,
            id instanceof GuestIdentification.ForeignGuest fg ? fg.passport() : null,
            id instanceof GuestIdentification.ForeignGuest fg ? fg.country() : null,
            reservation.getCheckIn().toString(),
            reservation.getCheckOut().toString(),
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
}
