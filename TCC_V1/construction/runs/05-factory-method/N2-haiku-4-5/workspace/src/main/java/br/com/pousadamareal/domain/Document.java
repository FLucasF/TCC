package br.com.pousadamareal.domain;

import br.com.pousadamareal.util.DateUtils;

public class Document {
    private String title;
    private Long reservationId;
    private String guestName;
    private String guestCpf;
    private String companyCnpj;
    private String companyName;
    private String guestPassport;
    private String guestCountry;
    private int roomNumber;
    private String roomType;
    private java.time.LocalDate checkInDate;
    private java.time.LocalDate checkOutDate;
    private int nights;
    private double subtotal;
    private Double discount;
    private double total;
    private String text;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Long getReservationId() {
        return reservationId;
    }

    public void setReservationId(Long reservationId) {
        this.reservationId = reservationId;
    }

    public String getGuestName() {
        return guestName;
    }

    public void setGuestName(String guestName) {
        this.guestName = guestName;
    }

    public String getGuestCpf() {
        return guestCpf;
    }

    public void setGuestCpf(String guestCpf) {
        this.guestCpf = guestCpf;
    }

    public String getCompanyCnpj() {
        return companyCnpj;
    }

    public void setCompanyCnpj(String companyCnpj) {
        this.companyCnpj = companyCnpj;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getGuestPassport() {
        return guestPassport;
    }

    public void setGuestPassport(String guestPassport) {
        this.guestPassport = guestPassport;
    }

    public String getGuestCountry() {
        return guestCountry;
    }

    public void setGuestCountry(String guestCountry) {
        this.guestCountry = guestCountry;
    }

    public int getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(int roomNumber) {
        this.roomNumber = roomNumber;
    }

    public String getRoomType() {
        return roomType;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    public java.time.LocalDate getCheckInDate() {
        return checkInDate;
    }

    public void setCheckInDate(java.time.LocalDate checkInDate) {
        this.checkInDate = checkInDate;
    }

    public java.time.LocalDate getCheckOutDate() {
        return checkOutDate;
    }

    public void setCheckOutDate(java.time.LocalDate checkOutDate) {
        this.checkOutDate = checkOutDate;
    }

    public int getNights() {
        return nights;
    }

    public void setNights(int nights) {
        this.nights = nights;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(double subtotal) {
        this.subtotal = subtotal;
    }

    public Double getDiscount() {
        return discount;
    }

    public void setDiscount(Double discount) {
        this.discount = discount;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getCheckIn() {
        return checkInDate != null ? DateUtils.toBrazilian(checkInDate) : null;
    }

    public String getCheckOut() {
        return checkOutDate != null ? DateUtils.toBrazilian(checkOutDate) : null;
    }
}
