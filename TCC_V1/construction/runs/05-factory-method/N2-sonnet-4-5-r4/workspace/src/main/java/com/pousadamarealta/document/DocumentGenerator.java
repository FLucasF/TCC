package com.pousadamarealta.document;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.RoomType;
import java.math.BigDecimal;
import java.time.LocalDate;

public abstract class DocumentGenerator {

    public final Document generate(Reservation reservation, RoomType roomType) {
        StringBuilder text = new StringBuilder();

        text.append(formatTitle(reservation.id())).append("\n");
        text.append(formatGuestIdentification(reservation)).append("\n");
        text.append(formatRoom(reservation.roomNumber(), roomType)).append("\n");
        text.append(formatPeriod(reservation.checkIn(), reservation.checkOut(), reservation.nights())).append("\n");
        text.append(formatSubtotal(reservation.subtotal())).append("\n");

        if (reservation.discount().compareTo(BigDecimal.ZERO) > 0) {
            text.append(formatDiscount(reservation.discount())).append("\n");
        }

        text.append(formatTotal(reservation.total())).append("\n");

        String additionalFields = formatAdditionalFields(reservation);
        if (!additionalFields.isEmpty()) {
            text.append(additionalFields).append("\n");
        }

        return Document.from(
            reservation.id(),
            reservation.guestName(),
            reservation.guestIdentification(),
            reservation.roomNumber(),
            roomType,
            reservation.checkIn(),
            reservation.checkOut(),
            reservation.nights(),
            reservation.subtotal(),
            reservation.discount(),
            reservation.total(),
            text.toString().trim()
        );
    }

    protected abstract String formatTitle(long reservationId);
    protected abstract String formatGuestIdentification(Reservation reservation);
    protected abstract String formatRoom(int roomNumber, RoomType roomType);
    protected abstract String formatPeriod(LocalDate checkIn, LocalDate checkOut, int nights);
    protected abstract String formatSubtotal(BigDecimal subtotal);
    protected abstract String formatDiscount(BigDecimal discount);
    protected abstract String formatTotal(BigDecimal total);
    protected abstract String formatAdditionalFields(Reservation reservation);
}
