package com.pousada.marealta.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class Receipt {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final int reservationId;
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

    public Receipt(int reservationId, String guestName, String guestCpf, int roomNumber, String roomType,
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

    public String format() {
        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(reservationId).append("\n");
        sb.append("Hóspede: ").append(guestName).append(" (CPF ").append(formatCpf(guestCpf)).append(")\n");
        sb.append("Quarto: ").append(roomNumber).append(" (").append(roomType).append(")\n");
        sb.append("Entrada: ").append(checkIn.format(DATE_FORMATTER)).append(" | ")
            .append("Saída: ").append(checkOut.format(DATE_FORMATTER)).append(" | ")
            .append(nights).append(nights == 1 ? " diária" : " diárias").append("\n");
        sb.append("Diárias: ").append(formatCurrency(subtotal)).append("\n");

        if (discount.compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(formatCurrency(discount)).append("\n");
        }

        sb.append("Total: ").append(formatCurrency(total)).append("\n");

        return sb.toString();
    }

    private String formatCpf(String cpf) {
        return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-" + cpf.substring(9);
    }

    private String formatCurrency(BigDecimal value) {
        String formatted = String.format("%.2f", value).replace(".", ",");
        return "R$ " + formatted;
    }

    public int getReservationId() {
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

    public String getRoomType() {
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
}
