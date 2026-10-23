package com.pousada.domain.guest;

import com.pousada.domain.model.Document;
import com.pousada.domain.model.Reservation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class CorporateGuest implements GuestType {
    private static final Locale PT_BR = new Locale.Builder().setLanguage("pt").setRegion("BR").build();
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(PT_BR);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final BigDecimal ISS_RATE = new BigDecimal("0.05");

    private final String cnpj;
    private final String companyName;

    public CorporateGuest(String cnpj, String companyName) {
        this.cnpj = cnpj;
        this.companyName = companyName;
    }

    @Override
    public Document generateDocument(Reservation reservation) {
        String text = buildText(reservation);

        return Document.builder()
            .title("Fatura de hospedagem")
            .reservationId(reservation.id())
            .guestName(reservation.guestName())
            .guestCpf(null)
            .companyCnpj(cnpj)
            .companyName(companyName)
            .guestPassport(null)
            .guestCountry(null)
            .roomNumber(reservation.roomNumber())
            .roomType(reservation.roomType().name())
            .checkIn(reservation.checkIn())
            .checkOut(reservation.checkOut())
            .nights(reservation.nights())
            .subtotal(reservation.subtotal())
            .discount(reservation.discount())
            .total(reservation.total())
            .text(text)
            .build();
    }

    private String buildText(Reservation r) {
        BigDecimal iss = r.total().multiply(ISS_RATE).setScale(2, RoundingMode.HALF_EVEN);

        StringBuilder sb = new StringBuilder();
        sb.append("Fatura de hospedagem nº ").append(r.id()).append("\n");
        sb.append("Empresa: ").append(companyName).append(" (CNPJ ").append(cnpj).append(")\n");
        sb.append("Hóspede: ").append(r.guestName()).append("\n");
        sb.append("Quarto: ").append(r.roomNumber()).append(" (").append(roomTypeName(r)).append(")\n");
        sb.append("Entrada: ").append(r.checkIn().format(DATE_FORMAT));
        sb.append(" | Saída: ").append(r.checkOut().format(DATE_FORMAT));
        sb.append(" | ").append(nightsText(r.nights())).append("\n");
        sb.append("Diárias: ").append(formatMoney(r.subtotal())).append("\n");

        if (r.discount() != null && r.discount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(formatMoney(r.discount())).append("\n");
        }

        sb.append("Total: ").append(formatMoney(r.total())).append("\n");
        sb.append("ISS retido (5%): ").append(formatMoney(iss));

        return sb.toString();
    }

    private String roomTypeName(Reservation r) {
        return r.roomType().name();
    }

    private String nightsText(int nights) {
        return nights == 1 ? "1 diária" : nights + " diárias";
    }

    private String formatMoney(BigDecimal amount) {
        return CURRENCY_FORMAT.format(amount);
    }

    @Override
    public String guestCpf() {
        return null;
    }

    @Override
    public String companyCnpj() {
        return cnpj;
    }

    @Override
    public String companyName() {
        return companyName;
    }

    @Override
    public String guestPassport() {
        return null;
    }

    @Override
    public String guestCountry() {
        return null;
    }
}
