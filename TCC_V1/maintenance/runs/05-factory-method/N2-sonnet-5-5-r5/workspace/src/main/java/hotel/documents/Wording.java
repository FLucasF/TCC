package hotel.documents;

import java.math.BigDecimal;
import java.time.LocalDate;

/** The language of a document: its fixed words and how money and dates are written in it. */
interface Wording {

    String money(BigDecimal amount);

    String date(LocalDate date);

    String numberSign();

    String guest();

    String room();

    String checkIn();

    String checkOut();

    String nights(int nights);

    String subtotal();

    String discount();

    String total();
}
