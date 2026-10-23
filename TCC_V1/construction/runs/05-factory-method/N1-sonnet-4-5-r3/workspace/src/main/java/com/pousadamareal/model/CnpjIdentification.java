package com.pousadamareal.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class CnpjIdentification implements GuestIdentification {
    private final String cnpj;
    private final String companyName;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final BigDecimal ISS_RATE = new BigDecimal("0.05");

    public CnpjIdentification(String cnpj, String companyName) {
        this.cnpj = cnpj;
        this.companyName = companyName;
    }

    @Override
    public String getGuestCpf() {
        return null;
    }

    @Override
    public String getCompanyCnpj() {
        return cnpj;
    }

    @Override
    public String getCompanyName() {
        return companyName;
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
        BigDecimal iss = total.multiply(ISS_RATE).setScale(2, RoundingMode.HALF_EVEN);

        StringBuilder text = new StringBuilder();
        text.append("Fatura de hospedagem nº ").append(reservationId).append("\n");
        text.append("Empresa: ").append(companyName).append(" (CNPJ ").append(cnpj).append(")\n");
        text.append("Hóspede: ").append(guestName).append("\n");
        text.append("Quarto: ").append(roomNumber).append(" (").append(roomType).append(")\n");
        text.append("Entrada: ").append(checkIn.format(DATE_FORMATTER))
            .append(" | Saída: ").append(checkOut.format(DATE_FORMATTER))
            .append(" | ").append(nights == 1 ? "1 diária" : nights + " diárias").append("\n");
        text.append("Diárias: ").append(formatMoney(subtotal)).append("\n");

        if (discount.compareTo(BigDecimal.ZERO) > 0) {
            text.append("Desconto: ").append(formatMoney(discount)).append("\n");
        }

        text.append("Total: ").append(formatMoney(total)).append("\n");
        text.append("ISS retido (5%): ").append(formatMoney(iss));

        return new Document(
            "Fatura de hospedagem",
            reservationId,
            guestName,
            null,
            cnpj,
            companyName,
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
