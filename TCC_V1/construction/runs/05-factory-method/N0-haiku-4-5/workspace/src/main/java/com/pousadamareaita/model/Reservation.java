package com.pousadamareaita.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDate;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Reservation {
    @JsonProperty("id")
    private long id;

    @JsonProperty("roomNumber")
    private int roomNumber;

    @JsonProperty("guestName")
    private String guestName;

    @JsonProperty("guestEmail")
    private String guestEmail;

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

    @JsonProperty("checkIn")
    private LocalDate checkIn;

    @JsonProperty("checkOut")
    private LocalDate checkOut;

    @JsonProperty("guests")
    private int guests;

    @JsonProperty("ratePlan")
    private String ratePlan;

    @JsonProperty("nights")
    private int nights;

    @JsonProperty("subtotal")
    private BigDecimal subtotal;

    @JsonProperty("discount")
    private BigDecimal discount;

    @JsonProperty("total")
    private BigDecimal total;

    @JsonProperty("status")
    private String status;

    @JsonProperty("amountPaid")
    private BigDecimal amountPaid;

    @JsonProperty("refund")
    private BigDecimal refund;

    public Reservation() {
    }

    public Reservation(long id, int roomNumber, String guestName, String guestEmail,
                       String guestCpf, String companyCnpj, String companyName,
                       String guestPassport, String guestCountry,
                       LocalDate checkIn, LocalDate checkOut, int guests, String ratePlan,
                       int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total,
                       String status, BigDecimal amountPaid, BigDecimal refund) {
        this.id = id;
        this.roomNumber = roomNumber;
        this.guestName = guestName;
        this.guestEmail = guestEmail;
        this.guestCpf = guestCpf;
        this.companyCnpj = companyCnpj;
        this.companyName = companyName;
        this.guestPassport = guestPassport;
        this.guestCountry = guestCountry;
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

    // Getters e setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public int getRoomNumber() { return roomNumber; }
    public void setRoomNumber(int roomNumber) { this.roomNumber = roomNumber; }

    public String getGuestName() { return guestName; }
    public void setGuestName(String guestName) { this.guestName = guestName; }

    public String getGuestEmail() { return guestEmail; }
    public void setGuestEmail(String guestEmail) { this.guestEmail = guestEmail; }

    public String getGuestCpf() { return guestCpf; }
    public void setGuestCpf(String guestCpf) { this.guestCpf = guestCpf; }

    public String getCompanyCnpj() { return companyCnpj; }
    public void setCompanyCnpj(String companyCnpj) { this.companyCnpj = companyCnpj; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getGuestPassport() { return guestPassport; }
    public void setGuestPassport(String guestPassport) { this.guestPassport = guestPassport; }

    public String getGuestCountry() { return guestCountry; }
    public void setGuestCountry(String guestCountry) { this.guestCountry = guestCountry; }

    public LocalDate getCheckIn() { return checkIn; }
    public void setCheckIn(LocalDate checkIn) { this.checkIn = checkIn; }

    public LocalDate getCheckOut() { return checkOut; }
    public void setCheckOut(LocalDate checkOut) { this.checkOut = checkOut; }

    public int getGuests() { return guests; }
    public void setGuests(int guests) { this.guests = guests; }

    public String getRatePlan() { return ratePlan; }
    public void setRatePlan(String ratePlan) { this.ratePlan = ratePlan; }

    public int getNights() { return nights; }
    public void setNights(int nights) { this.nights = nights; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public BigDecimal getDiscount() { return discount; }
    public void setDiscount(BigDecimal discount) { this.discount = discount; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public BigDecimal getAmountPaid() { return amountPaid; }
    public void setAmountPaid(BigDecimal amountPaid) { this.amountPaid = amountPaid; }

    public BigDecimal getRefund() { return refund; }
    public void setRefund(BigDecimal refund) { this.refund = refund; }
}
