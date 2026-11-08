package com.pousadamarealta.domain;

import com.pousadamarealta.util.Formatter;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ReservationDocument {
    private final String title = "Comprovante de reserva";
    private final long reservationId;
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
        this.subtotal = reservation.getSubtotal();
        this.discount = reservation.getDiscount();
        this.total = reservation.getTotal();
    }

    public String render() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Comprovante de reserva nº %d%n", reservationId));
        sb.append(String.format("Hóspede: %s (CPF %s)%n", guestName, Formatter.formatCpf(guestCpf)));
        sb.append(String.format("Quarto: %s (%s)%n", roomNumber, roomType.name()));

        String nightsText = nights == 1 ? "1 diária" : nights + " diárias";
        sb.append(String.format("Entrada: %s | Saída: %s | %s%n",
            Formatter.formatDate(checkIn),
            Formatter.formatDate(checkOut),
            nightsText));

        sb.append(String.format("Diárias: %s%n", Formatter.formatMoney(subtotal)));

        if (discount.compareTo(BigDecimal.ZERO) > 0) {
            sb.append(String.format("Desconto: %s%n", Formatter.formatMoney(discount)));
        }

        sb.append(String.format("Total: %s", Formatter.formatMoney(total)));

        return sb.toString();
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
