package com.pousada.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Receipt {
    private final String title;
    private final long reservationId;
    private final String guestName;
    private final String guestCpf;
    private final int roomNumber;
    private final RoomType roomType;
    private final LocalDate checkIn;
    private final LocalDate checkOut;
    private final int nights;
    private final BigDecimal subtotal;
    private final BigDecimal discount;
    private final BigDecimal total;

    public Receipt(long reservationId, String guestName, String guestCpf, int roomNumber,
                   RoomType roomType, LocalDate checkIn, LocalDate checkOut, int nights,
                   BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
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

    public int getRoomNumber() {
        return roomNumber;
    }

    public RoomType getRoomType() {
        return roomType;
    }

    public LocalDate getCheckIn() {
        return checkIn;
    }

    public LocalDate getCheckOut() {
        return checkOut;
    }

    public int getNights() {
        return nights;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public String formatAsText() {
        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(reservationId).append("\n");
        sb.append("Hóspede: ").append(guestName).append(" (CPF ").append(guestCpf).append(")\n");
        sb.append("Quarto: ").append(roomNumber).append(" (").append(roomType).append(")\n");
        sb.append("Entrada: ").append(formatDate(checkIn)).append(" | Saída: ").append(formatDate(checkOut))
                .append(" | ").append(nights).append(nights == 1 ? " diária" : " diárias").append("\n");
        sb.append("Diárias: R$ ").append(formatMoney(subtotal)).append("\n");
        if (discount.compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Desconto: R$ ").append(formatMoney(discount)).append("\n");
        }
        sb.append("Total: R$ ").append(formatMoney(total));
        return sb.toString();
    }

    private String formatDate(LocalDate date) {
        return String.format("%02d/%02d/%04d", date.getDayOfMonth(), date.getMonthValue(), date.getYear());
    }

    private String formatMoney(BigDecimal value) {
        return String.format("%.2f", value).replace(".", ",");
    }
}
