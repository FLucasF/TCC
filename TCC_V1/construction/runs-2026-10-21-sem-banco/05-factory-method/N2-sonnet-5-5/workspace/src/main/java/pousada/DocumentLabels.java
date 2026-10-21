package pousada;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

/** Idioma e formatação de um documento. */
public record DocumentLabels(String number, String room, String checkIn, String checkOut,
                             String nightOne, String nightMany, String subtotal, String discount,
                             String total, Function<BigDecimal, String> money, DateTimeFormatter date) {

    public static final DocumentLabels PORTUGUESE = new DocumentLabels(
            "nº", "Quarto", "Entrada", "Saída", "diária", "diárias", "Diárias", "Desconto", "Total",
            Money::brazilian, DateTimeFormatter.ofPattern("dd/MM/yyyy"));

    public static final DocumentLabels ENGLISH = new DocumentLabels(
            "no.", "Room", "Check-in", "Check-out", "night", "nights", "Nights", "Discount", "Total",
            Money::english, DateTimeFormatter.ISO_LOCAL_DATE);
}
