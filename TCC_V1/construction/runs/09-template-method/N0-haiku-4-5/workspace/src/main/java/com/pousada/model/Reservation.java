package com.pousada.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Reservation {
    private int id;
    private int roomNumber;
    private String guestName;
    private String guestEmail;
    private String guestCpf;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private int guests;
    private RatePlan ratePlan;
    private int nights;
    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal total;
    private ReservationStatus status;
    private BigDecimal amountPaid;
    private BigDecimal refund;

    public Reservation() {}

    public Reservation(int roomNumber, String guestName, String guestEmail, String guestCpf,
                       LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan,
                       int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
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
        this.amountPaid = BigDecimal.ZERO;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getRoomNumber() { return roomNumber; }
    public void setRoomNumber(int roomNumber) { this.roomNumber = roomNumber; }

    public String getGuestName() { return guestName; }
    public void setGuestName(String guestName) { this.guestName = guestName; }

    public String getGuestEmail() { return guestEmail; }
    public void setGuestEmail(String guestEmail) { this.guestEmail = guestEmail; }

    public String getGuestCpf() { return guestCpf; }
    public void setGuestCpf(String guestCpf) { this.guestCpf = guestCpf; }

    public LocalDate getCheckIn() { return checkIn; }
    public void setCheckIn(LocalDate checkIn) { this.checkIn = checkIn; }

    public LocalDate getCheckOut() { return checkOut; }
    public void setCheckOut(LocalDate checkOut) { this.checkOut = checkOut; }

    public int getGuests() { return guests; }
    public void setGuests(int guests) { this.guests = guests; }

    public RatePlan getRatePlan() { return ratePlan; }
    public void setRatePlan(RatePlan ratePlan) { this.ratePlan = ratePlan; }

    public int getNights() { return nights; }
    public void setNights(int nights) { this.nights = nights; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public BigDecimal getDiscount() { return discount; }
    public void setDiscount(BigDecimal discount) { this.discount = discount; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    public ReservationStatus getStatus() { return status; }
    public void setStatus(ReservationStatus status) { this.status = status; }

    public BigDecimal getAmountPaid() { return amountPaid; }
    public void setAmountPaid(BigDecimal amountPaid) { this.amountPaid = amountPaid; }

    public BigDecimal getRefund() { return refund; }
    public void setRefund(BigDecimal refund) { this.refund = refund; }
}
