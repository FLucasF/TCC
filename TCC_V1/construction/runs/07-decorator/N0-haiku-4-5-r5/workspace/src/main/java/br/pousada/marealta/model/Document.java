package br.pousada.marealta.model;

import com.google.gson.annotations.SerializedName;
import java.time.LocalDate;

public class Document {
    private String title;
    @SerializedName("reservationId")
    private long reservationId;
    @SerializedName("guestName")
    private String guestName;
    @SerializedName("guestCpf")
    private String guestCpf;
    @SerializedName("roomNumber")
    private int roomNumber;
    @SerializedName("roomType")
    private RoomType roomType;
    @SerializedName("checkIn")
    private LocalDate checkIn;
    @SerializedName("checkOut")
    private LocalDate checkOut;
    private int nights;
    private double subtotal;
    private Double discount;
    private double total;

    public Document(long reservationId, String guestName, String guestCpf, int roomNumber,
                    RoomType roomType, LocalDate checkIn, LocalDate checkOut, int nights,
                    double subtotal, Double discount, double total) {
        this.title = "Comprovante de reserva";
        this.reservationId = reservationId;
        this.guestName = guestName;
        this.guestCpf = guestCpf;
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.nights = nights;
        this.subtotal = subtotal;
        this.discount = discount;
        this.total = total;
    }

    public String getTitle() { return title; }
    public long getReservationId() { return reservationId; }
    public String getGuestName() { return guestName; }
    public String getGuestCpf() { return guestCpf; }
    public int getRoomNumber() { return roomNumber; }
    public RoomType getRoomType() { return roomType; }
    public LocalDate getCheckIn() { return checkIn; }
    public LocalDate getCheckOut() { return checkOut; }
    public int getNights() { return nights; }
    public double getSubtotal() { return subtotal; }
    public Double getDiscount() { return discount; }
    public double getTotal() { return total; }
}
