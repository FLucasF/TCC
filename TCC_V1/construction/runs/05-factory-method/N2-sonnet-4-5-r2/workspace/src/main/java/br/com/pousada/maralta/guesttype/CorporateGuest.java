package br.com.pousada.maralta.guesttype;

import br.com.pousada.maralta.domain.Document;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class CorporateGuest implements GuestType {
    private final String cnpj;
    private final String companyName;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DecimalFormat CURRENCY_FORMAT;
    private static final BigDecimal ISS_RATE = new BigDecimal("0.05");

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("pt", "BR"));
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        CURRENCY_FORMAT = new DecimalFormat("R$ #,##0.00", symbols);
    }

    public CorporateGuest(String cnpj, String companyName) {
        this.cnpj = cnpj;
        this.companyName = companyName;
    }

    @Override
    public String getGuestIdentificationLine() {
        return "CNPJ " + cnpj;
    }

    @Override
    public Document generateDocument(
        long reservationId,
        String guestName,
        String roomNumber,
        String roomType,
        LocalDate checkIn,
        LocalDate checkOut,
        int nights,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal total
    ) {
        BigDecimal iss = total.multiply(ISS_RATE).setScale(2, RoundingMode.HALF_EVEN);

        StringBuilder text = new StringBuilder();
        text.append("Fatura de hospedagem nº ").append(reservationId).append("\n");
        text.append("Empresa: ").append(companyName).append(" (CNPJ ").append(cnpj).append(")").append("\n");
        text.append("Hóspede: ").append(guestName).append("\n");
        text.append("Quarto: ").append(roomNumber).append(" (").append(roomType).append(")").append("\n");
        text.append("Entrada: ").append(checkIn.format(DATE_FORMAT))
            .append(" | Saída: ").append(checkOut.format(DATE_FORMAT))
            .append(" | ").append(nights).append(nights == 1 ? " diária" : " diárias").append("\n");
        text.append("Diárias: ").append(CURRENCY_FORMAT.format(subtotal)).append("\n");

        if (discount.compareTo(BigDecimal.ZERO) > 0) {
            text.append("Desconto: ").append(CURRENCY_FORMAT.format(discount)).append("\n");
        }

        text.append("Total: ").append(CURRENCY_FORMAT.format(total)).append("\n");
        text.append("ISS retido (5%): ").append(CURRENCY_FORMAT.format(iss));

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
}
