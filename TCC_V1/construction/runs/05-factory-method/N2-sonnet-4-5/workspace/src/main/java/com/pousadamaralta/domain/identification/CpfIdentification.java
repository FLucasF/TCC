package com.pousadamaralta.domain.identification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class CpfIdentification extends GuestIdentification {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final String cpf;

    public CpfIdentification(String cpf) {
        this.cpf = cpf;
    }

    @Override
    protected String getDocumentTitle() {
        return "Comprovante de reserva";
    }

    @Override
    protected String formatGuestLine(String guestName) {
        return "Hóspede: " + guestName + " (CPF " + cpf + ")";
    }

    @Override
    protected String formatMoney(BigDecimal amount) {
        return String.format("R$ %,.2f", amount);
    }

    @Override
    protected String formatDate(LocalDate date) {
        return date.format(DATE_FORMATTER);
    }

    @Override
    public String getCpf() {
        return cpf;
    }

    @Override
    public String getCnpj() {
        return null;
    }

    @Override
    public String getCompanyName() {
        return null;
    }

    @Override
    public String getPassport() {
        return null;
    }

    @Override
    public String getCountry() {
        return null;
    }
}
