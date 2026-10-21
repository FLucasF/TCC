package br.com.marealta.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class Reservation {
    private final long id;
    private final Room room;
    private final String guestName;
    private final String guestEmail;
    private final String guestCpf;
    private final LocalDate checkIn;
    private final LocalDate checkOut;
    private final int guests;
    private final RatePlan ratePlan;
    private final int nights;
    private final BigDecimal subtotal;
    private final BigDecimal discount;
    private final BigDecimal total;

    private ReservationStatus status = ReservationStatus.PENDING;
    private BigDecimal amountPaid = Formats.round(BigDecimal.ZERO);
    private BigDecimal refund;

    public Reservation(long id, Room room, String guestName, String guestEmail, String guestCpf,
                       LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {
        this.id = id;
        this.room = room;
        this.guestName = guestName;
        this.guestEmail = guestEmail;
        this.guestCpf = guestCpf;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.guests = guests;
        this.ratePlan = ratePlan;
        this.nights = (int) ChronoUnit.DAYS.between(checkIn, checkOut);
        this.subtotal = checkIn.datesUntil(checkOut)
                .map(room::priceOf)
                .reduce(Formats.round(BigDecimal.ZERO), BigDecimal::add);
        this.discount = ratePlan.discountOn(subtotal);
        this.total = Formats.round(subtotal.subtract(discount));
    }

    public void confirm() {
        requireStatus(ReservationStatus.PENDING);
        status = ReservationStatus.CONFIRMED;
        amountPaid = total;
    }

    public void checkIn() {
        requireStatus(ReservationStatus.CONFIRMED);
        status = ReservationStatus.CHECKED_IN;
    }

    public void checkOut() {
        requireStatus(ReservationStatus.CHECKED_IN);
        status = ReservationStatus.CHECKED_OUT;
    }

    public void cancel() {
        requireStatus(ReservationStatus.PENDING, ReservationStatus.CONFIRMED);
        status = ReservationStatus.CANCELLED;
        refund = ratePlan.refundOf(amountPaid);
    }

    private void requireStatus(ReservationStatus... allowed) {
        for (ReservationStatus option : allowed) {
            if (status == option) {
                return;
            }
        }
        throw new DomainException(ErrorCode.OPERATION_NOT_ALLOWED,
                "Operação não permitida para reserva com status " + status);
    }

    /** Dinheiro que ficou com a pousada: o que foi pago menos o que foi devolvido. */
    public BigDecimal amountKept() {
        return refund == null ? amountPaid : amountPaid.subtract(refund);
    }

    /** Se a reserva ocupa o quarto na noite que começa em {@code night}. */
    public boolean occupies(LocalDate night) {
        return !night.isBefore(checkIn) && night.isBefore(checkOut);
    }

    public boolean overlaps(LocalDate otherCheckIn, LocalDate otherCheckOut) {
        return otherCheckIn.isBefore(checkOut) && checkIn.isBefore(otherCheckOut);
    }

    public Receipt receipt() {
        if (!status.isPaid()) {
            throw new DomainException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Comprovante só existe para reserva paga");
        }
        return new Receipt(Receipt.TITLE, id, guestName, guestCpf, room.number(), room.type(),
                checkIn, checkOut, nights, subtotal, discount, total);
    }

    public long id() { return id; }
    public Room room() { return room; }
    public String guestName() { return guestName; }
    public String guestEmail() { return guestEmail; }
    public String guestCpf() { return guestCpf; }
    public LocalDate checkInDate() { return checkIn; }
    public LocalDate checkOutDate() { return checkOut; }
    public int guests() { return guests; }
    public RatePlan ratePlan() { return ratePlan; }
    public int nights() { return nights; }
    public BigDecimal subtotal() { return subtotal; }
    public BigDecimal discount() { return discount; }
    public BigDecimal total() { return total; }
    public ReservationStatus status() { return status; }
    public BigDecimal amountPaid() { return amountPaid; }
    public BigDecimal refund() { return refund; }
}
