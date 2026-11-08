package pousada.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

import static pousada.domain.ReservationStatus.*;

public record Reservation(
        long id,
        int roomNumber,
        String guestName,
        String guestEmail,
        String guestCpf,
        LocalDate checkIn,
        LocalDate checkOut,
        int guests,
        String ratePlan,
        int nights,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal total,
        ReservationStatus status,
        BigDecimal amountPaid,
        BigDecimal refund) {

    public Reservation confirm() {
        requireStatus("confirmada", PENDING);
        return with(CONFIRMED, total, refund);
    }

    public Reservation startStay() {
        requireStatus("receber check-in", CONFIRMED);
        return with(CHECKED_IN, amountPaid, refund);
    }

    public Reservation endStay() {
        requireStatus("receber check-out", CHECKED_IN);
        return with(CHECKED_OUT, amountPaid, refund);
    }

    public Reservation cancel() {
        requireStatus("cancelada", PENDING, CONFIRMED);
        return with(CANCELLED, amountPaid, RatePlans.get(ratePlan).refund(amountPaid));
    }

    public Receipt receipt(Room room) {
        if (!status.isPaid()) {
            throw DomainException.operationNotAllowed("Só reservas pagas têm comprovante.");
        }
        return new Receipt("Comprovante de reserva", id, guestName, guestCpf, roomNumber, room.type(),
                checkIn, checkOut, nights, subtotal, discount, total);
    }

    private void requireStatus(String action, ReservationStatus... allowed) {
        for (ReservationStatus s : allowed) {
            if (status == s) {
                return;
            }
        }
        throw DomainException.operationNotAllowed(
                "A reserva " + id + " está " + status + " e não pode ser " + action + ".");
    }

    private Reservation with(ReservationStatus newStatus, BigDecimal newAmountPaid, BigDecimal newRefund) {
        return new Reservation(id, roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests,
                ratePlan, nights, subtotal, discount, total, newStatus, newAmountPaid, newRefund);
    }
}
