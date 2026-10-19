package com.pousadameraalta.api.dto;

import com.pousadameraalta.model.Reservation;
import com.pousadameraalta.util.DocumentGenerator;
import java.math.BigDecimal;

public class DocumentResponse {
    public String title;
    public long reservationId;
    public String guestName;
    public String guestCpf;
    public String companyCnpj;
    public String companyName;
    public String guestPassport;
    public String guestCountry;
    public int roomNumber;
    public String roomType;
    public String checkIn;
    public String checkOut;
    public int nights;
    public BigDecimal subtotal;
    public BigDecimal discount;
    public BigDecimal total;
    public String text;

    public static DocumentResponse fromReservation(Reservation r) {
        DocumentResponse resp = new DocumentResponse();
        resp.title = "Comprovante de reserva";
        resp.reservationId = r.getId();
        resp.guestName = r.getGuestName();
        resp.guestCpf = r.getGuestCpf();
        resp.companyCnpj = r.getCompanyCnpj();
        resp.companyName = r.getCompanyName();
        resp.guestPassport = r.getGuestPassport();
        resp.guestCountry = r.getGuestCountry();
        resp.roomNumber = r.getRoomNumber();
        resp.roomType = getQuartType(r.getRoomNumber());
        resp.checkIn = r.getCheckIn().toString();
        resp.checkOut = r.getCheckOut().toString();
        resp.nights = r.getNights();
        resp.subtotal = r.getSubtotal();
        resp.discount = r.getDiscount();
        resp.total = r.getTotal();

        if (r.getGuestCpf() != null) {
            resp.text = DocumentGenerator.generateReceipt(r);
        } else if (r.getCompanyCnpj() != null) {
            resp.text = DocumentGenerator.generateInvoice(r);
        } else {
            resp.text = DocumentGenerator.generateBookingReceipt(r);
        }

        return resp;
    }

    private static String getQuartType(int roomNumber) {
        return switch (roomNumber) {
            case 101, 102 -> "STANDARD";
            case 201, 202 -> "DELUXE";
            case 301 -> "SUITE";
            default -> "UNKNOWN";
        };
    }
}
