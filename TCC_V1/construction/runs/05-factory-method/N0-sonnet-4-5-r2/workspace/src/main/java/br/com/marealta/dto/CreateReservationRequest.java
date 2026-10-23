package br.com.marealta.dto;

public record CreateReservationRequest(
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
    Integer guests,
    String ratePlan
) {
}
