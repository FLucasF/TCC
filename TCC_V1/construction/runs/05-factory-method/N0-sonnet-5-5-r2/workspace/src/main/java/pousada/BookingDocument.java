package pousada;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Issued document (receipt, invoice or booking receipt) with its rendered text. */
public record BookingDocument(
        String title,
        long reservationId,
        String guestName,
        GuestIdentity identity,
        int roomNumber,
        RoomType roomType,
        LocalDate checkIn,
        LocalDate checkOut,
        int nights,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal total,
        String text) {}
