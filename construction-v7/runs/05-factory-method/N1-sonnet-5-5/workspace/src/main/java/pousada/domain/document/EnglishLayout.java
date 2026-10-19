package pousada.domain.document;

import pousada.domain.Money;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Disposição em inglês: BRL 1,234.56 e 2027-03-10. */
public final class EnglishLayout {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private EnglishLayout() {
    }

    public static String render(String title, Voucher v, List<String> header, List<String> footer) {
        List<String> lines = new ArrayList<>();
        lines.add(title + " no. " + v.reservationId());
        lines.addAll(header);
        lines.add("Room: " + v.roomNumber() + " (" + v.roomType() + ")");
        lines.add("Check-in: " + DATE.format(v.checkIn()) + " | Check-out: " + DATE.format(v.checkOut())
                + " | " + v.nights() + (v.nights() == 1 ? " night" : " nights"));
        lines.add("Nights: " + Money.brlEnglish(v.subtotal()));
        if (v.hasDiscount()) {
            lines.add("Discount: " + Money.brlEnglish(v.discount()));
        }
        lines.add("Total: " + Money.brlEnglish(v.total()));
        lines.addAll(footer);
        return String.join("\n", lines);
    }
}
