package com.pousadamaralta.domain.identification;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class CnpjIdentification extends GuestIdentification {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final BigDecimal ISS_RATE = new BigDecimal("0.05");

    private final String cnpj;
    private final String companyName;

    public CnpjIdentification(String cnpj, String companyName) {
        this.cnpj = cnpj;
        this.companyName = companyName;
    }

    @Override
    protected String getDocumentTitle() {
        return "Fatura de hospedagem";
    }

    @Override
    protected String formatGuestLine(String guestName) {
        return "Empresa: " + companyName + " (CNPJ " + cnpj + ")\n" +
               "Hóspede: " + guestName;
    }

    @Override
    protected String getExtraInfo(BigDecimal total) {
        BigDecimal iss = total.multiply(ISS_RATE).setScale(2, RoundingMode.HALF_EVEN);
        return "ISS retido (5%): " + formatMoney(iss);
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
        return null;
    }

    @Override
    public String getCnpj() {
        return cnpj;
    }

    @Override
    public String getCompanyName() {
        return companyName;
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
