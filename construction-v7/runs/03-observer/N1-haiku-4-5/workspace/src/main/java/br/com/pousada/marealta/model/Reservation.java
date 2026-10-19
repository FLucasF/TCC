package br.com.pousada.marealta.model;

import java.time.LocalDate;

public class Reservation {
    private final long id;
    private final int roomNumber;
    private final String guestName;
    private final String guestEmail;
    private final String guestCpf;
    private final LocalDate checkIn;
    private final LocalDate checkOut;
    private final int guests;
    private final RatePlan ratePlan;
    private final int nights;
    private final double subtotal;
    private final double discount;
    private final double total;
    private ReservationStatus status;
    private double amountPaid;
    private Double refund;

    public Reservation(
            long id,
            int roomNumber,
            String guestName,
            String guestEmail,
            String guestCpf,
            LocalDate checkIn,
            LocalDate checkOut,
            int guests,
            RatePlan ratePlan,
            int nights,
            double subtotal,
            double discount,
            double total
    ) {
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
        this.status = ReservationStatus.PENDING;
        this.amountPaid = 0;
        this.refund = null;
    }

    public long getId() { return id; }
    public int getRoomNumber() { return roomNumber; }
    public String getGuestName() { return guestName; }
    public String getGuestEmail() { return guestEmail; }
    public String getGuestCpf() { return guestCpf; }
    public LocalDate getCheckIn() { return checkIn; }
    public LocalDate getCheckOut() { return checkOut; }
    public int getGuests() { return guests; }
    public RatePlan getRatePlan() { return ratePlan; }
    public int getNights() { return nights; }
    public double getSubtotal() { return subtotal; }
    public double getDiscount() { return discount; }
    public double getTotal() { return total; }
    public ReservationStatus getStatus() { return status; }
    public double getAmountPaid() { return amountPaid; }
    public Double getRefund() { return refund; }

    public void confirm(double amountPaid) {
        this.status = ReservationStatus.CONFIRMED;
        this.amountPaid = amountPaid;
    }

    public void checkIn() {
        this.status = ReservationStatus.CHECKED_IN;
    }

    public void checkOut() {
        this.status = ReservationStatus.CHECKED_OUT;
    }

    public void cancel(Double refundAmount) {
        this.status = ReservationStatus.CANCELLED;
        this.refund = refundAmount;
    }
}
