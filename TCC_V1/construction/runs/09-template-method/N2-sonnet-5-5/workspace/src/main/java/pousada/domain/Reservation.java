package pousada.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Reservation {
    private Long id;
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

    public static Reservation create(Room room, String guestName, String guestEmail, String guestCpf,
                                     LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {
        Pricing.Quote quote = Pricing.quote(room, checkIn, checkOut, ratePlan);
        return new Reservation(null, room.number(), guestName, guestEmail, guestCpf, checkIn, checkOut, guests,
                ratePlan, quote.nights(), quote.subtotal(), quote.discount(), quote.total(),
                ReservationStatus.PENDING, Money.round(BigDecimal.ZERO), null);
    }

    public void confirm() {
        require(status == ReservationStatus.PENDING, "Só é possível confirmar uma reserva pendente");
        status = ReservationStatus.CONFIRMED;
        amountPaid = total;
    }

    public void cancel() {
        require(status == ReservationStatus.PENDING || status == ReservationStatus.CONFIRMED,
                "Só é possível cancelar antes do check-in");
        refund = ratePlan.refund(amountPaid);
        status = ReservationStatus.CANCELLED;
    }

    public void checkIn() {
        require(status == ReservationStatus.CONFIRMED, "Só é possível fazer check-in de uma reserva confirmada");
        status = ReservationStatus.CHECKED_IN;
    }

    public void checkOut() {
        require(status == ReservationStatus.CHECKED_IN, "Só é possível fazer check-out após o check-in");
        status = ReservationStatus.CHECKED_OUT;
    }

    private static void require(boolean allowed, String message) {
        if (!allowed) {
            throw new AppException(ErrorCode.OPERATION_NOT_ALLOWED, message);
        }
    }

    public boolean paid() {
        return status.paid();
    }

    /** Dinheiro que ficou com a pousada: o pago menos o devolvido. */
    public BigDecimal retained() {
        return refund == null ? amountPaid : Money.round(amountPaid.subtract(refund));
    }

    /** Ocupa o quarto na noite informada (entrada conta, saída não). */
    public boolean occupies(LocalDate night) {
        return paid() && !checkIn.isAfter(night) && night.isBefore(checkOut);
    }

    public void assignId(long id) {
        this.id = id;
    }

    public Long getId() { return id; }
    public int getRoomNumber() { return roomNumber; }
    public String getGuestName() { return guestName; }
    public String getGuestEmail() { return guestEmail; }
    public String getGuestCpf() { return guestCpf; }
    public LocalDate getCheckIn() { return checkIn; }
    public LocalDate getCheckOut() { return checkOut; }
    public int getGuests() { return guests; }
    public RatePlan getRatePlan() { return ratePlan; }
    public int getNights() { return nights; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getDiscount() { return discount; }
    public BigDecimal getTotal() { return total; }
    public ReservationStatus getStatus() { return status; }
    public BigDecimal getAmountPaid() { return amountPaid; }
    public BigDecimal getRefund() { return refund; }
}
