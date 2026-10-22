package br.pousada.marealta.model;

import com.google.gson.annotations.SerializedName;
import java.time.LocalDate;

public class Reservation {
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
    private RatePlan ratePlan;
    private int nights;
    private double subtotal;
    private double discount;
    private double total;
    private ReservationStatus status;
    @SerializedName("amountPaid")
    private double amountPaid;
    private Double refund;

    public Reservation() {}

    public Reservation(long id, int roomNumber, String guestName, String guestEmail, String guestCpf,
                       LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan,
                       int nights, double subtotal, double discount, double total,
                       ReservationStatus status, double amountPaid, Double refund) {
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
    public RatePlan getRatePlan() { return ratePlan; }
    public int getNights() { return nights; }
    public double getSubtotal() { return subtotal; }
    public double getDiscount() { return discount; }
    public double getTotal() { return total; }
    public ReservationStatus getStatus() { return status; }
    public double getAmountPaid() { return amountPaid; }
    public Double getRefund() { return refund; }

    public void setStatus(ReservationStatus status) { this.status = status; }
    public void setAmountPaid(double amountPaid) { this.amountPaid = amountPaid; }
    public void setRefund(Double refund) { this.refund = refund; }
}
