package com.pousadamareaita.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDate;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Document {
    @JsonProperty("title")
    private String title;

    @JsonProperty("reservationId")
    private long reservationId;

    @JsonProperty("guestName")
    private String guestName;

    @JsonProperty("guestCpf")
    private String guestCpf;

    @JsonProperty("companyCnpj")
    private String companyCnpj;

    @JsonProperty("companyName")
    private String companyName;

    @JsonProperty("guestPassport")
    private String guestPassport;

    @JsonProperty("guestCountry")
    private String guestCountry;

    @JsonProperty("roomNumber")
    private int roomNumber;

    @JsonProperty("roomType")
    private String roomType;

    @JsonProperty("checkIn")
    private LocalDate checkIn;

    @JsonProperty("checkOut")
    private LocalDate checkOut;

    @JsonProperty("nights")
    private int nights;

    @JsonProperty("subtotal")
    private BigDecimal subtotal;

    @JsonProperty("discount")
    private BigDecimal discount;

    @JsonProperty("total")
    private BigDecimal total;

    @JsonProperty("text")
    private String text;

    public Document(String title, long reservationId, String guestName,
                    String guestCpf, String companyCnpj, String companyName,
                    String guestPassport, String guestCountry,
                    int roomNumber, String roomType, LocalDate checkIn, LocalDate checkOut,
                    int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total, String text) {
        this.title = title;
        this.reservationId = reservationId;
        this.guestName = guestName;
        this.guestCpf = guestCpf;
        this.companyCnpj = companyCnpj;
        this.companyName = companyName;
        this.guestPassport = guestPassport;
        this.guestCountry = guestCountry;
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.nights = nights;
        this.subtotal = subtotal;
        this.discount = discount;
        this.total = total;
        this.text = text;
    }

    public String getTitle() { return title; }
    public long getReservationId() { return reservationId; }
    public String getGuestName() { return guestName; }
    public String getGuestCpf() { return guestCpf; }
    public String getCompanyCnpj() { return companyCnpj; }
    public String getCompanyName() { return companyName; }
    public String getGuestPassport() { return guestPassport; }
    public String getGuestCountry() { return guestCountry; }
    public int getRoomNumber() { return roomNumber; }
    public String getRoomType() { return roomType; }
    public LocalDate getCheckIn() { return checkIn; }
    public LocalDate getCheckOut() { return checkOut; }
    public int getNights() { return nights; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getDiscount() { return discount; }
    public BigDecimal getTotal() { return total; }
    public String getText() { return text; }
}
