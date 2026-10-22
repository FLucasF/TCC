package com.marealta.pousada.persistence;

import com.marealta.pousada.domain.Reservation;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository {
    /** Grava a reserva nova e devolve o número atribuído a ela. */
    long insert(Reservation reservation);

    void update(Reservation reservation);

    Optional<Reservation> find(long id);

    /** Reservas não canceladas do quarto que ocupam alguma noite entre checkIn (inclusive) e checkOut (exclusive). */
    List<Reservation> findActiveOverlapping(int roomNumber, LocalDate checkIn, LocalDate checkOut);

    /** Reservas de qualquer situação que ocupam alguma noite entre checkIn (inclusive) e checkOut (exclusive). */
    List<Reservation> findOverlapping(LocalDate checkIn, LocalDate checkOut);

    /** Reservas cuja entrada cai entre as duas datas, inclusive. */
    List<Reservation> findByCheckInBetween(LocalDate from, LocalDate to);
}
