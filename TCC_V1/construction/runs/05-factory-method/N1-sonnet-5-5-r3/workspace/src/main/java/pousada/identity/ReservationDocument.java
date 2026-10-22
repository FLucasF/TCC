package pousada.identity;

import pousada.domain.Reservation;

import java.util.ArrayList;
import java.util.List;

/** O documento (comprovante, fatura, booking receipt) de uma reserva paga: a sequência é uma só. */
public final class ReservationDocument {

    private ReservationDocument() {
    }

    public static String title(Reservation r) {
        return r.identity().documentTitle();
    }

    public static String text(Reservation r) {
        GuestIdentity identity = r.identity();
        DocumentLanguage lang = identity.language();
        List<String> lines = new ArrayList<>();
        lines.add(identity.documentTitle() + " " + lang.numberSign() + " " + r.id());
        lines.addAll(identity.partyLines(r.guestName()));
        lines.add(lang.roomLine(r.room().number(), r.room().type()));
        lines.add(lang.stayLine(r.checkIn(), r.checkOut(), r.quote().nights()));
        lines.add(lang.subtotalLine(r.quote().subtotal()));
        if (r.quote().discount().signum() > 0) {
            lines.add(lang.discountLine(r.quote().discount()));
        }
        lines.add(lang.totalLine(r.quote().total()));
        lines.addAll(identity.footerLines(r.quote().total()));
        return String.join("\n", lines);
    }
}
