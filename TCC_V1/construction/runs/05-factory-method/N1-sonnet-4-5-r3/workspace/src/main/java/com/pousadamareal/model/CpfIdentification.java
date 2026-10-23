package com.pousadamareal.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class CpfIdentification implements GuestIdentification {
    private final String cpf;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public CpfIdentification(String cpf) {
        this.cpf = cpf;
    }

    @Override
    public String getGuestCpf() {
        return cpf;
    }

    @Override
    public String getCompanyCnpj() {
        return null;
    }

    @Override
    public String getCompanyName() {
        return null;
    }

    @Override
    public String getGuestPassport() {
        return null;
    }

    @Override
    public String getGuestCountry() {
        return null;
    }

    @Override
    public Document generateDocument(long reservationId, String guestName, String roomNumber,
                                    RoomType roomType, LocalDate checkIn, LocalDate checkOut,
                                    int nights, BigDecimal subtotal, BigDecimal discount,
                                    BigDecimal total) {
        StringBuilder text = new StringBuilder();
        text.append("Comprovante de reserva nº ").append(reservationId).append("\n");
        text.append("Hóspede: ").append(guestName).append(" (CPF ").append(cpf).append(")\n");
        text.append("Quarto: ").append(roomNumber).append(" (").append(roomType).append(")\n");
        text.append("Entrada: ").append(checkIn.format(DATE_FORMATTER))
            .append(" | Saída: ").append(checkOut.format(DATE_FORMATTER))
            .append(" | ").append(nights == 1 ? "1 diária" : nights + " diárias").append("\n");
        text.append("Diárias: ").append(formatMoney(subtotal)).append("\n");

        if (discount.compareTo(BigDecimal.ZERO) > 0) {
            text.append("Desconto: ").append(formatMoney(discount)).append("\n");
        }

        text.append("Total: ").append(formatMoney(total));

        return new Document(
            "Comprovante de reserva",
            reservationId,
            guestName,
            cpf,
            null,
            null,
            null,
            null,
            roomNumber,
            roomType,
            checkIn,
            checkOut,
            nights,
            subtotal,
            discount,
            total,
            text.toString()
        );
    }

    private String formatMoney(BigDecimal amount) {
        return String.format("R$ %,.2f", amount).replace(",", "X").replace(".", ",").replace("X", ".");
    }
}
