package com.pousada.models;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Receipt {
    private final long reservationId;
    private final String guestName;
    private final String guestCpf;
    private final int roomNumber;
    private final String roomType;
    private final LocalDate checkIn;
    private final LocalDate checkOut;
    private final int nights;
    private final BigDecimal subtotal;
    private final BigDecimal discount;
    private final BigDecimal total;

    public Receipt(long reservationId, String guestName, String guestCpf, int roomNumber, String roomType,
                  LocalDate checkIn, LocalDate checkOut, int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
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

    public long getReservationId() { return reservationId; }
    public String getGuestName() { return guestName; }
    public String getGuestCpf() { return guestCpf; }
    public int getRoomNumber() { return roomNumber; }
    public String getRoomType() { return roomType; }
    public LocalDate getCheckIn() { return checkIn; }
    public LocalDate getCheckOut() { return checkOut; }
    public int getNights() { return nights; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getDiscount() { return discount; }
    public BigDecimal getTotal() { return total; }

    public String format() {
        String nightsText = nights == 1 ? "1 diária" : nights + " diárias";
        String checkInFormatted = formatDate(checkIn);
        String checkOutFormatted = formatDate(checkOut);
        String subtotalFormatted = formatMoney(subtotal);
        String discountFormatted = discount != null && discount.signum() > 0 ? formatMoney(discount) : null;
        String totalFormatted = formatMoney(total);

        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(reservationId).append("\n");
        sb.append("Hóspede: ").append(guestName).append(" (CPF ").append(guestCpf).append(")\n");
        sb.append("Quarto: ").append(roomNumber).append(" (").append(roomType).append(")\n");
        sb.append("Entrada: ").append(checkInFormatted).append(" | Saída: ").append(checkOutFormatted).append(" | ").append(nightsText).append("\n");
        sb.append("Diárias: ").append(subtotalFormatted).append("\n");
        if (discountFormatted != null) {
            sb.append("Desconto: ").append(discountFormatted).append("\n");
        }
        sb.append("Total: ").append(totalFormatted);
        return sb.toString();
    }

    private String formatDate(LocalDate date) {
        int day = date.getDayOfMonth();
        int month = date.getMonthValue();
        int year = date.getYear();
        return String.format("%02d/%02d/%04d", day, month, year);
    }

    private String formatMoney(BigDecimal value) {
        value = value.setScale(2, java.math.RoundingMode.HALF_UP);
        return String.format("R$ %,.2f", value).replace(",", "X").replace(".", ",").replace("X", ".");
    }
}
