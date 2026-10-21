package com.pousada.domain;

import java.math.BigDecimal;

public class Document {
    private final String title;
    private final long reservationId;
    private final String guestName;
    private final String guestCpf;
    private final String companyName;
    private final String companyCnpj;
    private final String guestPassport;
    private final String guestCountry;
    private final String roomNumber;
    private final RoomType roomType;
    private final String checkIn;
    private final String checkOut;
    private final int nights;
    private final String subtotal;
    private final String discount;
    private final String total;
    private final String text;

    public Document(String title, long reservationId, GuestIdentification identification,
                    String roomNumber, RoomType roomType, String checkIn, String checkOut,
                    int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
        this.title = title;
        this.reservationId = reservationId;
        this.guestName = identification.getGuestName();

        if (identification instanceof CpfIdentification cpf) {
            this.guestCpf = cpf.guestCpf();
            this.companyName = null;
            this.companyCnpj = null;
            this.guestPassport = null;
            this.guestCountry = null;
        } else if (identification instanceof CnpjIdentification cnpj) {
            this.guestCpf = null;
            this.companyName = cnpj.companyName();
            this.companyCnpj = cnpj.companyCnpj();
            this.guestPassport = null;
            this.guestCountry = null;
        } else if (identification instanceof PassportIdentification passport) {
            this.guestCpf = null;
            this.companyName = null;
            this.companyCnpj = null;
            this.guestPassport = passport.guestPassport();
            this.guestCountry = passport.guestCountry();
        } else {
            throw new IllegalArgumentException("Unknown identification type");
        }

        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.nights = nights;
        this.subtotal = formatMoney(subtotal);
        this.discount = discount != null ? formatMoney(discount) : null;
        this.total = formatMoney(total);

        DocumentData data = new DocumentData(
            reservationId, roomNumber, roomType.name(), checkIn, checkOut,
            nights, subtotal, discount, total
        );
        this.text = identification.generateDocument(data);
    }

    private String formatMoney(BigDecimal value) {
        return String.format("%,.2f", value).replace(',', 'X').replace('.', ',').replace('X', '.');
    }

    public String getTitle() {
        return title;
    }

    public long getReservationId() {
        return reservationId;
    }

    public String getGuestName() {
        return guestName;
    }

    public String getGuestCpf() {
        return guestCpf;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getCompanyCnpj() {
        return companyCnpj;
    }

    public String getGuestPassport() {
        return guestPassport;
    }

    public String getGuestCountry() {
        return guestCountry;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public RoomType getRoomType() {
        return roomType;
    }

    public String getCheckIn() {
        return checkIn;
    }

    public String getCheckOut() {
        return checkOut;
    }

    public int getNights() {
        return nights;
    }

    public String getSubtotal() {
        return subtotal;
    }

    public String getDiscount() {
        return discount;
    }

    public String getTotal() {
        return total;
    }

    public String getText() {
        return text;
    }

    public record DocumentData(
        long reservationId,
        String roomNumber,
        String roomType,
        String checkIn,
        String checkOut,
        int nights,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal total
    ) {}
}
