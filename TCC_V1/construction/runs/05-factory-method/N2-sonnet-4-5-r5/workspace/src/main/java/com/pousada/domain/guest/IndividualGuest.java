package com.pousada.domain.guest;

import com.pousada.domain.model.Document;
import com.pousada.domain.model.Reservation;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class IndividualGuest implements GuestType {
    private static final Locale PT_BR = new Locale.Builder().setLanguage("pt").setRegion("BR").build();
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(PT_BR);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final String cpf;

    public IndividualGuest(String cpf) {
        this.cpf = cpf;
    }

    @Override
    public Document generateDocument(Reservation reservation) {
        String text = buildText(reservation);

        return Document.builder()
            .title("Comprovante de reserva")
            .reservationId(reservation.id())
            .guestName(reservation.guestName())
            .guestCpf(cpf)
            .companyCnpj(null)
            .companyName(null)
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
        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(r.id()).append("\n");
        sb.append("Hóspede: ").append(r.guestName()).append(" (CPF ").append(cpf).append(")\n");
        sb.append("Quarto: ").append(r.roomNumber()).append(" (").append(roomTypeName(r)).append(")\n");
        sb.append("Entrada: ").append(r.checkIn().format(DATE_FORMAT));
        sb.append(" | Saída: ").append(r.checkOut().format(DATE_FORMAT));
        sb.append(" | ").append(nightsText(r.nights())).append("\n");
        sb.append("Diárias: ").append(formatMoney(r.subtotal())).append("\n");

        if (r.discount() != null && r.discount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(formatMoney(r.discount())).append("\n");
        }

        sb.append("Total: ").append(formatMoney(r.total()));

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
        return cpf;
    }

    @Override
    public String companyCnpj() {
        return null;
    }

    @Override
    public String companyName() {
        return null;
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
