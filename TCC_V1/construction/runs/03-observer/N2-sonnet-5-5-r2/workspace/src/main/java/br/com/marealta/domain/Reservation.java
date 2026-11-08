package br.com.marealta.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class Reservation {

    private long id;
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
    private ReservationStatus status;
    private BigDecimal amountPaid;
    private BigDecimal refund;

    private Reservation(long id, Room room, String guestName, String guestEmail, String guestCpf,
                        LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan,
                        int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total,
                        ReservationStatus status, BigDecimal amountPaid, BigDecimal refund) {
        this.id = id;
        this.room = room;
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

    /** Uma reserva nova: pendente, com o preço calculado. A numeração é dada ao gravar. */
    public static Reservation create(Room room, String guestName, String guestEmail, String guestCpf,
                                     LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {
        if (!checkOut.isAfter(checkIn)) {
            throw new PousadaException(ErrorCode.INVALID_REQUEST, "A saída deve ser depois da entrada");
        }
        if (guests < 1) {
            throw new PousadaException(ErrorCode.INVALID_REQUEST, "É preciso ao menos um hóspede");
        }
        if (guests > room.capacity()) {
            throw new PousadaException(ErrorCode.CAPACITY_EXCEEDED,
                    "O quarto " + room.number() + " comporta no máximo " + room.capacity() + " hóspedes");
        }
        Pricing.Quote quote = Pricing.quote(room, checkIn, checkOut, ratePlan);
        return new Reservation(0, room, guestName, guestEmail, guestCpf, checkIn, checkOut, guests, ratePlan,
                quote.nights(), quote.subtotal(), quote.discount(), quote.total(),
                ReservationStatus.PENDING, Money.ZERO, null);
    }

    /** Reconstrói uma reserva guardada. */
    public static Reservation restore(long id, Room room, String guestName, String guestEmail, String guestCpf,
                                      LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan,
                                      int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total,
                                      ReservationStatus status, BigDecimal amountPaid, BigDecimal refund) {
        return new Reservation(id, room, guestName, guestEmail, guestCpf, checkIn, checkOut, guests, ratePlan,
                nights, subtotal, discount, total, status, amountPaid, refund);
    }

    public void confirm() {
        requireStatus("confirmar", ReservationStatus.PENDING);
        status = ReservationStatus.CONFIRMED;
        amountPaid = total;
    }

    public void checkIn() {
        requireStatus("fazer o check-in de", ReservationStatus.CONFIRMED);
        status = ReservationStatus.CHECKED_IN;
    }

    public void checkOut() {
        requireStatus("fazer o check-out de", ReservationStatus.CHECKED_IN);
        status = ReservationStatus.CHECKED_OUT;
    }

    public void cancel() {
        requireStatus("cancelar", ReservationStatus.PENDING, ReservationStatus.CONFIRMED);
        refund = ratePlan.refund(amountPaid);
        status = ReservationStatus.CANCELLED;
    }

    public Receipt receipt() {
        if (!isPaid()) {
            throw new PousadaException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Só existe comprovante de reserva paga; a reserva está " + status);
        }
        return new Receipt(Receipt.TITLE, id, guestName, guestCpf, room.number(), room.type(),
                checkIn, checkOut, nights, subtotal, discount, total);
    }

    /** Paga e não cancelada: confirmada, com check-in ou com check-out. */
    public boolean isPaid() {
        return status == ReservationStatus.CONFIRMED
                || status == ReservationStatus.CHECKED_IN
                || status == ReservationStatus.CHECKED_OUT;
    }

    /** Já houve pagamento (mesmo que depois tenha sido cancelada). */
    public boolean hasPayment() {
        return amountPaid.signum() > 0;
    }

    /** O que ficou com a pousada: pago menos devolvido. */
    public BigDecimal retained() {
        return refund == null ? amountPaid : amountPaid.subtract(refund);
    }

    private void requireStatus(String operation, ReservationStatus... allowed) {
        for (ReservationStatus s : allowed) {
            if (status == s) {
                return;
            }
        }
        throw new PousadaException(ErrorCode.OPERATION_NOT_ALLOWED,
                "Não é possível " + operation + " uma reserva " + status);
    }

    public void assignId(long id) {
        this.id = id;
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
