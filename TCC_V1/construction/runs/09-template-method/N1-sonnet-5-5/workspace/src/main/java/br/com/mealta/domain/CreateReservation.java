package br.com.mealta.domain;

import java.time.LocalDate;

public record CreateReservation(int roomNumber, String guestName, String guestEmail, String guestCpf,
                                LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {
}
