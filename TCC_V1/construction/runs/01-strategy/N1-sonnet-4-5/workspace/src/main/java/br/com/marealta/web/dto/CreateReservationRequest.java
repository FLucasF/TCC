package br.com.marealta.web.dto;

public record CreateReservationRequest(
    String roomNumber,
    String guestName,
    String guestEmail,
    String guestCpf,
    String checkIn,
    String checkOut,
    Integer guests,
    String ratePlan,
    String companyCode
) {}
