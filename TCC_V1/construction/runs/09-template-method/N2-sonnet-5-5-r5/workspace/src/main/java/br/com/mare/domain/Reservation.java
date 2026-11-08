package br.com.mare.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;

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

    /** Nova reserva pendente, com os valores já calculados. */
    public static Reservation book(Room room, String guestName, String guestEmail, String guestCpf,
                                   LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {
        int nights = (int) ChronoUnit.DAYS.between(checkIn, checkOut);
        BigDecimal subtotal = Stream.iterate(checkIn, d -> d.plusDays(1))
                .limit(nights)
                .map(room::priceOfNight)
                .reduce(Money.ZERO, BigDecimal::add);
        BigDecimal discount = ratePlan.discount(subtotal);
        BigDecimal total = Money.round(subtotal.subtract(discount));
        return new Reservation(null, room.number(), guestName, guestEmail, guestCpf, checkIn, checkOut,
                guests, ratePlan, nights, subtotal, discount, total,
                ReservationStatus.PENDING, Money.ZERO, null);
    }

    public void confirm() {
        status = status.confirm();
        amountPaid = total;
    }

    public void registerCheckIn() {
        status = status.checkIn();
    }

    public void registerCheckOut() {
        status = status.checkOut();
    }

    public void cancel() {
        status = status.cancel();
        refund = ratePlan.refund(amountPaid);
    }

    /** O que ficou com a pousada: o que foi pago menos o que foi devolvido. */
    public BigDecimal retained() {
        return refund == null ? amountPaid : Money.round(amountPaid.subtract(refund));
    }

    public boolean isPaid() {
        return status.isPaid();
    }

    /** Ocupa a noite que começa em {@code night}: quem entra conta, quem sai no dia não. */
    public boolean occupies(LocalDate night) {
        return !night.isBefore(checkIn) && night.isBefore(checkOut);
    }

    public boolean overlaps(LocalDate otherCheckIn, LocalDate otherCheckOut) {
        return checkIn.isBefore(otherCheckOut) && otherCheckIn.isBefore(checkOut);
    }

    public Long id() { return id; }
    public void assignId(long id) { this.id = id; }
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
