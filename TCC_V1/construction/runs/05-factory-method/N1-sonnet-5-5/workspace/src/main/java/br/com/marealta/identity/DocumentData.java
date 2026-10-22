package br.com.marealta.identity;

import br.com.marealta.domain.Quote;

import java.time.LocalDate;

public record DocumentData(long reservationId, String guestName, GuestIdentity identity, int roomNumber,
                           String roomType, LocalDate checkIn, LocalDate checkOut, Quote quote) {}
