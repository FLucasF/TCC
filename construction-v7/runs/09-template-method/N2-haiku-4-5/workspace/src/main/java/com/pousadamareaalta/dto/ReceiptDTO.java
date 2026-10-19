package com.pousadamareaalta.dto;

import com.pousadamareaalta.model.Receipt;

public class ReceiptDTO {
    public String title;
    public long reservationId;
    public String guestName;
    public String guestCpf;
    public int roomNumber;
    public String roomType;
    public String checkIn;
    public String checkOut;
    public int nights;
    public double subtotal;
    public double discount;
    public double total;

    public static ReceiptDTO fromReceipt(Receipt receipt) {
        ReceiptDTO dto = new ReceiptDTO();
        dto.title = receipt.getTitle();
        dto.reservationId = receipt.getReservationId();
        dto.guestName = receipt.getGuestName();
        dto.guestCpf = receipt.getGuestCpf();
        dto.roomNumber = receipt.getRoomNumber();
        dto.roomType = receipt.getRoomType();
        dto.checkIn = receipt.getCheckIn().toString();
        dto.checkOut = receipt.getCheckOut().toString();
        dto.nights = receipt.getNights();
        dto.subtotal = receipt.getSubtotal().doubleValue();
        dto.discount = receipt.getDiscount().doubleValue();
        dto.total = receipt.getTotal().doubleValue();
        return dto;
    }
}
