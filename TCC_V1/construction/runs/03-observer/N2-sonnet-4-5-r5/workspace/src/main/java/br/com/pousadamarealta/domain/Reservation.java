package br.com.pousadamarealta.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public final class Reservation {
    private final Long id;
    private final String roomNumber;
    private final RoomType roomType;
    private final int roomCapacity;
    private final BigDecimal roomNightlyRate;
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

    public Reservation(Long id, Room room, String guestName, String guestEmail, String guestCpf,
                       LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {
        if (checkOut.isBefore(checkIn) || checkOut.equals(checkIn)) {
            throw new IllegalArgumentException("Check-out must be after check-in");
        }
        if (guests < 1) {
            throw new IllegalArgumentException("At least one guest is required");
        }
        if (guests > room.capacity()) {
            throw new IllegalArgumentException("Guests exceed room capacity");
        }

        this.id = id;
        this.roomNumber = room.number();
        this.roomType = room.type();
        this.roomCapacity = room.capacity();
        this.roomNightlyRate = room.nightlyRate();
        this.guestName = guestName;
        this.guestEmail = guestEmail;
        this.guestCpf = guestCpf;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.guests = guests;
        this.ratePlan = ratePlan;
        this.nights = (int) ChronoUnit.DAYS.between(checkIn, checkOut);
        this.subtotal = PriceCalculator.calculateSubtotal(checkIn, checkOut, room.nightlyRate());
        this.discount = PriceCalculator.calculateDiscount(subtotal, ratePlan);
        this.total = PriceCalculator.calculateTotal(subtotal, discount);
        this.status = ReservationStatus.PENDING;
        this.amountPaid = BigDecimal.ZERO;
        this.refund = null;
    }

    public Reservation(Long id, String roomNumber, RoomType roomType, int roomCapacity, BigDecimal roomNightlyRate,
                       String guestName, String guestEmail, String guestCpf, LocalDate checkIn, LocalDate checkOut,
                       int guests, RatePlan ratePlan, int nights, BigDecimal subtotal, BigDecimal discount,
                       BigDecimal total, ReservationStatus status, BigDecimal amountPaid, BigDecimal refund) {
        this.id = id;
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.roomCapacity = roomCapacity;
        this.roomNightlyRate = roomNightlyRate;
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

    public void confirm() {
        if (status != ReservationStatus.PENDING) {
            throw new IllegalStateException("Only pending reservations can be confirmed");
        }
        this.status = ReservationStatus.CONFIRMED;
        this.amountPaid = this.total;
    }

    public void performCheckIn() {
        if (status != ReservationStatus.CONFIRMED) {
            throw new IllegalStateException("Only confirmed reservations can be checked in");
        }
        this.status = ReservationStatus.CHECKED_IN;
    }

    public void performCheckOut() {
        if (status != ReservationStatus.CHECKED_IN) {
            throw new IllegalStateException("Only checked-in reservations can be checked out");
        }
        this.status = ReservationStatus.CHECKED_OUT;
    }

    public void cancel() {
        if (status != ReservationStatus.PENDING && status != ReservationStatus.CONFIRMED) {
            throw new IllegalStateException("Only pending or confirmed reservations can be cancelled");
        }

        BigDecimal refundAmount = BigDecimal.ZERO;
        if (status == ReservationStatus.CONFIRMED && ratePlan == RatePlan.STANDARD) {
            refundAmount = amountPaid;
        }

        this.status = ReservationStatus.CANCELLED;
        this.refund = refundAmount;
    }

    public boolean occupiesNight(LocalDate night) {
        return (night.equals(checkIn) || night.isAfter(checkIn)) && night.isBefore(checkOut);
    }

    public Long id() { return id; }
    public String roomNumber() { return roomNumber; }
    public RoomType roomType() { return roomType; }
    public int roomCapacity() { return roomCapacity; }
    public BigDecimal roomNightlyRate() { return roomNightlyRate; }
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
