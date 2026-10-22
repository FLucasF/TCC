package pousada.identity;

import pousada.domain.RoomType;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Idioma e formato (dinheiro, datas, rótulos) em que um documento é escrito. */
public enum DocumentLanguage {
    PORTUGUESE(Locale.forLanguageTag("pt-BR"), "nº", "R$ ", "dd/MM/yyyy",
            "Quarto", "diária", "diárias", "Entrada: %s | Saída: %s | %d %s",
            "Diárias", "Desconto", "Total"),
    ENGLISH(Locale.US, "no.", "BRL ", "yyyy-MM-dd",
            "Room", "night", "nights", "Check-in: %s | Check-out: %s | %d %s",
            "Nights", "Discount", "Total");

    private final Locale locale;
    private final String numberSign;
    private final String currencyPrefix;
    private final DateTimeFormatter dateFormat;
    private final String roomLabel;
    private final String nightSingular;
    private final String nightPlural;
    private final String stayPattern;
    private final String subtotalLabel;
    private final String discountLabel;
    private final String totalLabel;

    DocumentLanguage(Locale locale, String numberSign, String currencyPrefix, String datePattern,
                     String roomLabel, String nightSingular, String nightPlural, String stayPattern,
                     String subtotalLabel, String discountLabel, String totalLabel) {
        this.locale = locale;
        this.numberSign = numberSign;
        this.currencyPrefix = currencyPrefix;
        this.dateFormat = DateTimeFormatter.ofPattern(datePattern);
        this.roomLabel = roomLabel;
        this.nightSingular = nightSingular;
        this.nightPlural = nightPlural;
        this.stayPattern = stayPattern;
        this.subtotalLabel = subtotalLabel;
        this.discountLabel = discountLabel;
        this.totalLabel = totalLabel;
    }

    public String numberSign() {
        return numberSign;
    }

    public String money(BigDecimal value) {
        DecimalFormat format = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(locale));
        return currencyPrefix + format.format(value);
    }

    public String roomLine(int number, RoomType type) {
        return roomLabel + ": " + number + " (" + type + ")";
    }

    public String stayLine(LocalDate checkIn, LocalDate checkOut, int nights) {
        return stayPattern.formatted(dateFormat.format(checkIn), dateFormat.format(checkOut), nights,
                nights == 1 ? nightSingular : nightPlural);
    }

    public String subtotalLine(BigDecimal value) {
        return subtotalLabel + ": " + money(value);
    }

    public String discountLine(BigDecimal value) {
        return discountLabel + ": " + money(value);
    }

    public String totalLine(BigDecimal value) {
        return totalLabel + ": " + money(value);
    }
}
