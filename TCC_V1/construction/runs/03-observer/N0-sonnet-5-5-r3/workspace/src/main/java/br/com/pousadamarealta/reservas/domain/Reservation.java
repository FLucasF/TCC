package br.com.pousadamarealta.reservas.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

import static br.com.pousadamarealta.reservas.domain.ReservationStatus.CANCELLED;
import static br.com.pousadamarealta.reservas.domain.ReservationStatus.CHECKED_IN;
import static br.com.pousadamarealta.reservas.domain.ReservationStatus.CHECKED_OUT;
import static br.com.pousadamarealta.reservas.domain.ReservationStatus.CONFIRMED;
import static br.com.pousadamarealta.reservas.domain.ReservationStatus.PENDING;

/** Reserva imutável; cada transição devolve uma nova instância. {@code id} é null até ser gravada. */
public record Reservation(
        Long id,
        int roomNumber,
        String guestName,
        String guestEmail,
        String guestCpf,
        LocalDate checkIn,
        LocalDate checkOut,
        int guests,
        RatePlan ratePlan,
        int nights,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal total,
        ReservationStatus status,
        BigDecimal amountPaid,
        BigDecimal refund) {

    public static Reservation create(Room room, String guestName, String guestEmail, String guestCpf,
                                     LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {
        if (guests < 1) {
            throw new DomainException(ErrorCode.INVALID_REQUEST, "guests deve ser pelo menos 1");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new DomainException(ErrorCode.INVALID_REQUEST, "checkOut deve ser depois de checkIn");
        }
        if (guests > room.capacity()) {
            throw new DomainException(ErrorCode.CAPACITY_EXCEEDED,
                    "O quarto " + room.number() + " comporta no máximo " + room.capacity() + " hóspedes");
        }
        Pricing.Quote quote = Pricing.quote(room, checkIn, checkOut, ratePlan);
        return new Reservation(null, room.number(), guestName, guestEmail, guestCpf, checkIn, checkOut, guests,
                ratePlan, quote.nights(), quote.subtotal(), quote.discount(), quote.total(),
                PENDING, Money.ZERO, null);
    }

    public Reservation confirm() {
        requireStatus("confirmar", PENDING);
        return copy(CONFIRMED, total, refund);
    }

    public Reservation doCheckIn() {
        requireStatus("fazer check-in de", CONFIRMED);
        return copy(CHECKED_IN, amountPaid, refund);
    }

    public Reservation doCheckOut() {
        requireStatus("fazer check-out de", CHECKED_IN);
        return copy(CHECKED_OUT, amountPaid, refund);
    }

    public Reservation cancel() {
        requireStatus("cancelar", PENDING, CONFIRMED);
        BigDecimal toRefund = status == CONFIRMED && ratePlan.refundable() ? amountPaid : Money.ZERO;
        return copy(CANCELLED, amountPaid, toRefund);
    }

    /** O que ficou com a pousada: pago menos devolvido. */
    public BigDecimal retained() {
        return refund == null ? amountPaid : amountPaid.subtract(refund);
    }

    private void requireStatus(String action, ReservationStatus... allowed) {
        for (ReservationStatus s : allowed) {
            if (status == s) {
                return;
            }
        }
        throw new DomainException(ErrorCode.OPERATION_NOT_ALLOWED,
                "Não é possível " + action + " uma reserva com status " + status);
    }

    private Reservation copy(ReservationStatus newStatus, BigDecimal newPaid, BigDecimal newRefund) {
        return new Reservation(id, roomNumber, guestName, guestEmail, guestCpf, checkIn, checkOut, guests,
                ratePlan, nights, subtotal, discount, total, newStatus, newPaid, newRefund);
    }
}
