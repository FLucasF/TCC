package com.pousadamarealta.api.response;

import com.google.gson.annotations.SerializedName;
import java.math.BigDecimal;
import java.time.LocalDate;

public class ReservationResponse {
    private long id;
    @SerializedName("roomNumber")
    private int roomNumber;
    @SerializedName("guestName")
    private String guestName;
    @SerializedName("guestEmail")
    private String guestEmail;
    @SerializedName("guestCpf")
    private String guestCpf;
    @SerializedName("checkIn")
    private LocalDate checkIn;
    @SerializedName("checkOut")
    private LocalDate checkOut;
    private int guests;
    @SerializedName("ratePlan")
    private String ratePlan;
    private int nights;
    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal total;
    private String status;
    @SerializedName("amountPaid")
    private BigDecimal amountPaid;
    private BigDecimal refund;

    public ReservationResponse(long id, int roomNumber, String guestName, String guestEmail, String guestCpf,
                               LocalDate checkIn, LocalDate checkOut, int guests, String ratePlan,
                               int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total,
                               String status, BigDecimal amountPaid, BigDecimal refund) {
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

    public long getId() { return id; }
    public int getRoomNumber() { return roomNumber; }
    public String getGuestName() { return guestName; }
    public String getGuestEmail() { return guestEmail; }
    public String getGuestCpf() { return guestCpf; }
    public LocalDate getCheckIn() { return checkIn; }
    public LocalDate getCheckOut() { return checkOut; }
    public int getGuests() { return guests; }
    public String getRatePlan() { return ratePlan; }
    public int getNights() { return nights; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getDiscount() { return discount; }
    public BigDecimal getTotal() { return total; }
    public String getStatus() { return status; }
    public BigDecimal getAmountPaid() { return amountPaid; }
    public BigDecimal getRefund() { return refund; }
}
