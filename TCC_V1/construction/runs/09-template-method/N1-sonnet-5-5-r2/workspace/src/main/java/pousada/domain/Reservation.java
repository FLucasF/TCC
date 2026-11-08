package pousada.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

public class Reservation {
    private final Long id;
    private final int roomNumber;
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
    private ReservationStatus status;
    private BigDecimal amountPaid;
    private BigDecimal refund;

    public Reservation(Long id, int roomNumber, String guestName, String guestEmail, String guestCpf,
                       LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan,
                       int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total,
                       ReservationStatus status, BigDecimal amountPaid, BigDecimal refund) {
        this.id = id;
        this.roomNumber = roomNumber;
        this.guestName = guestName;
        this.guestEmail = guestEmail;
        this.guestCpf = guestCpf;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.guests = guests;
        this.ratePlan = ratePlan;
        this.nights = nights;
        this.subtotal = subtotal;
        this.discount = discount;
        this.total = total;
        this.status = status;
        this.amountPaid = amountPaid;
        this.refund = refund;
    }

    /** Reserva nova, pendente e com os valores já calculados. */
    public static Reservation open(Room room, NewReservation request) {
        Quote quote = Quote.of(room, request.checkIn(), request.checkOut(), request.ratePlan());
        return new Reservation(null, room.number(), request.guestName(), request.guestEmail(),
                request.guestCpf(), request.checkIn(), request.checkOut(), request.guests(),
                request.ratePlan(), quote.nights(), quote.subtotal(), quote.discount(), quote.total(),
                ReservationStatus.PENDING, Money.ZERO, null);
    }

    public void confirm() {
        requireStatus("confirmada", ReservationStatus.PENDING);
        amountPaid = total;
        status = ReservationStatus.CONFIRMED;
    }

    public void registerCheckIn() {
        requireStatus("receber check-in", ReservationStatus.CONFIRMED);
        status = ReservationStatus.CHECKED_IN;
    }

    public void registerCheckOut() {
        requireStatus("receber check-out", ReservationStatus.CHECKED_IN);
        status = ReservationStatus.CHECKED_OUT;
    }

    public void cancel() {
        requireStatus("cancelada", ReservationStatus.PENDING, ReservationStatus.CONFIRMED);
        refund = ratePlan.refundFor(amountPaid);
        status = ReservationStatus.CANCELLED;
    }

    public void requirePaid() {
        if (!status.isPaid()) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Só reservas pagas têm comprovante; esta está " + status);
        }
    }

    /** O que ficou com a pousada: o pago menos o devolvido. */
    public BigDecimal retained() {
        return refund == null ? amountPaid : amountPaid.subtract(refund);
    }

    public boolean occupies(LocalDate night) {
        return !night.isBefore(checkIn) && night.isBefore(checkOut);
    }

    private void requireStatus(String action, ReservationStatus... allowed) {
        if (!Set.of(allowed).contains(status)) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "A reserva está " + status + " e não pode ser " + action);
        }
    }

    public Long id() { return id; }
    public int roomNumber() { return roomNumber; }
    public String guestName() { return guestName; }
    public String guestEmail() { return guestEmail; }
    public String guestCpf() { return guestCpf; }
    public LocalDate checkIn() { return checkIn; }
    public LocalDate checkOut() { return checkOut; }
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
