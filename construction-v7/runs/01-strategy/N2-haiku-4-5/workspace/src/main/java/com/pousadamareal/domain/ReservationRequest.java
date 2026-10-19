package com.pousadamareal.domain;

import java.time.LocalDate;

public record ReservationRequest(
    int roomNumber,
    String guestName,
    String guestEmail,
    String guestCpf,
    LocalDate checkIn,
    LocalDate checkOut,
    int guests,
    String ratePlan,
    String companyCode
) {}
