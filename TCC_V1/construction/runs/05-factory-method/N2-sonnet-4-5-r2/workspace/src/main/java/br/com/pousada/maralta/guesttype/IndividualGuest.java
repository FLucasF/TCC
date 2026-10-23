package br.com.pousada.maralta.guesttype;

import br.com.pousada.maralta.domain.Document;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class IndividualGuest implements GuestType {
    private final String cpf;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DecimalFormat CURRENCY_FORMAT;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("pt", "BR"));
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        CURRENCY_FORMAT = new DecimalFormat("R$ #,##0.00", symbols);
    }

    public IndividualGuest(String cpf) {
        this.cpf = cpf;
    }

    @Override
    public String getGuestIdentificationLine() {
        return "CPF " + cpf;
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
        StringBuilder text = new StringBuilder();
        text.append("Comprovante de reserva nº ").append(reservationId).append("\n");
        text.append("Hóspede: ").append(guestName).append(" (CPF ").append(cpf).append(")").append("\n");
        text.append("Quarto: ").append(roomNumber).append(" (").append(roomType).append(")").append("\n");
        text.append("Entrada: ").append(checkIn.format(DATE_FORMAT))
            .append(" | Saída: ").append(checkOut.format(DATE_FORMAT))
            .append(" | ").append(nights).append(nights == 1 ? " diária" : " diárias").append("\n");
        text.append("Diárias: ").append(CURRENCY_FORMAT.format(subtotal)).append("\n");

        if (discount.compareTo(BigDecimal.ZERO) > 0) {
            text.append("Desconto: ").append(CURRENCY_FORMAT.format(discount)).append("\n");
        }

        text.append("Total: ").append(CURRENCY_FORMAT.format(total));

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
}
