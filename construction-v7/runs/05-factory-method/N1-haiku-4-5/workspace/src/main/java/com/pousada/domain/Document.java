package com.pousada.domain;

import java.math.BigDecimal;

public class Document {
    private final String title;
    private final long reservationId;
    private final String guestName;
    private final CpfIdentification guestCpfId;
    private final CnpjIdentification companyCnpjId;
    private final PassportIdentification guestPassportId;
    private final int roomNumber;
    private final String roomType;
    private final String checkIn;
    private final String checkOut;
    private final long nights;
    private final BigDecimal subtotal;
    private final BigDecimal discount;
    private final BigDecimal total;
    private final String text;
    private final BigDecimal issRetained;

    public Document(String title, long reservationId, String guestName,
                   CpfIdentification guestCpfId, CnpjIdentification companyCnpjId,
                   PassportIdentification guestPassportId, int roomNumber, String roomType,
                   String checkIn, String checkOut, long nights,
                   BigDecimal subtotal, BigDecimal discount, BigDecimal total, String text) {
        this(title, reservationId, guestName, guestCpfId, companyCnpjId, guestPassportId,
             roomNumber, roomType, checkIn, checkOut, nights, subtotal, discount, total, text, null);
    }

    public Document(String title, long reservationId, String guestName,
                   CpfIdentification guestCpfId, CnpjIdentification companyCnpjId,
                   PassportIdentification guestPassportId, int roomNumber, String roomType,
                   String checkIn, String checkOut, long nights,
                   BigDecimal subtotal, BigDecimal discount, BigDecimal total, String text,
                   BigDecimal issRetained) {
        this.title = title;
        this.reservationId = reservationId;
        this.guestName = guestName;
        this.guestCpfId = guestCpfId;
        this.companyCnpjId = companyCnpjId;
        this.guestPassportId = guestPassportId;
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.nights = nights;
        this.subtotal = subtotal;
        this.discount = discount;
        this.total = total;
        this.text = text;
        this.issRetained = issRetained;
    }

    public String getTitle() { return title; }
    public long getReservationId() { return reservationId; }
    public String getGuestName() { return guestName; }
    public CpfIdentification getGuestCpfId() { return guestCpfId; }
    public CnpjIdentification getCompanyCnpjId() { return companyCnpjId; }
    public PassportIdentification getGuestPassportId() { return guestPassportId; }
    public int getRoomNumber() { return roomNumber; }
    public String getRoomType() { return roomType; }
    public String getCheckIn() { return checkIn; }
    public String getCheckOut() { return checkOut; }
    public long getNights() { return nights; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getDiscount() { return discount; }
    public BigDecimal getTotal() { return total; }
    public String getText() { return text; }
    public BigDecimal getIssRetained() { return issRetained; }
}
