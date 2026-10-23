package com.pousadamarealta.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class ReservationDocument {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final Long reservationId;
    private final String guestName;
    private final String guestCpf;
    private final String roomNumber;
    private final RoomType roomType;
    private final LocalDate checkIn;
    private final LocalDate checkOut;
    private final int nights;
    private final BigDecimal subtotal;
    private final BigDecimal discount;
    private final BigDecimal total;

    public ReservationDocument(Reservation reservation) {
        this.reservationId = reservation.getId();
        this.guestName = reservation.getGuestName();
        this.guestCpf = reservation.getGuestCpf();
        this.roomNumber = reservation.getRoomNumber();
        this.roomType = reservation.getRoomType();
        this.checkIn = reservation.getCheckIn();
        this.checkOut = reservation.getCheckOut();
        this.nights = reservation.getNights();
        this.subtotal = reservation.calculateSubtotal();
        this.discount = reservation.calculateDiscount();
        this.total = reservation.calculateTotal();
    }

    public String formatAsText() {
        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(reservationId).append("\n");
        sb.append("Hóspede: ").append(guestName).append(" (CPF ").append(formatCpf(guestCpf)).append(")\n");
        sb.append("Quarto: ").append(roomNumber).append(" (").append(roomType).append(")\n");
        sb.append("Entrada: ").append(checkIn.format(DATE_FORMATTER))
          .append(" | Saída: ").append(checkOut.format(DATE_FORMATTER))
          .append(" | ").append(nights).append(nights == 1 ? " diária" : " diárias").append("\n");
        sb.append("Diárias: ").append(formatMoney(subtotal)).append("\n");

        if (discount.compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(formatMoney(discount)).append("\n");
        }

        sb.append("Total: ").append(formatMoney(total));

        return sb.toString();
    }

    private String formatCpf(String cpf) {
        if (cpf.length() == 11) {
            return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." +
                   cpf.substring(6, 9) + "-" + cpf.substring(9, 11);
        }
        return cpf;
    }

    private String formatMoney(BigDecimal amount) {
        return String.format("R$ %,.2f", amount).replace(",", "X").replace(".", ",").replace("X", ".");
    }

    public Long getReservationId() {
        return reservationId;
    }

    public String getGuestName() {
        return guestName;
    }

    public String getGuestCpf() {
        return guestCpf;
    }

    public String getRoomNumber() {
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
}
